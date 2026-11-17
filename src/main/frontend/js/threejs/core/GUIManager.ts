import * as THREE from 'three';
import type { OrbitControls } from 'three/addons';
import { createCameraActions, type CameraActions } from './CameraActions';

/**
 * GUI controls for interactive camera manipulation
 *
 * Provides buttons for common camera operations:
 * - Rotate up/down/left/right
 * - Zoom in/out
 * - Reset camera
 * - Show/hide GUI
 *
 * Every button drives the shared {@link CameraActions}, which the keyboard bindings on the canvas use
 * too, so the two routes cannot drift apart.
 *
 * The arrows and the magnifiers used to listen for mousedown, mouseup and mouseleave and nothing else.
 * They are real <button> elements, so focus reached them, but Enter and the space bar did nothing at
 * all: only the centring button had a click listener. They now respond to a click for one step and to
 * a held key for a run of them.
 *
 * The strings here are Czech literals rather than keys from texts/*_cs.json. The panel is built in
 * plain DOM inside the Three.js layer, with no access to the Vaadin i18n provider; unifying that means
 * passing the strings in from Java, which is a larger change than this brief allows.
 */
export class GUIManager {
    /** WCAG 2.5.8: the 3 × 3 grid was 122 px wide, which left each button under 40 px. */
    private static readonly BUTTON_SIZE = 44;
    private static readonly GRID_GAP = 4;

    private gui: HTMLElement | null = null;
    private intervalId: number | null = null;
    private backgroundSyncHandler: ((ev: Event) => void) | null = null;
    private cameraActions: CameraActions | null = null;

    /**
     * Create interactive GUI controls for camera manipulation
     * 
     * Generates a stylized control panel with buttons for:
     * - Rotation: Smooth continuous rotation around model
     * - Pan: Directional camera movement (up/down/left/right)
     * - Zoom: Incremental zoom in/out
     * - Reset: Return to initial view centered on model
     * - Background: Choose color, sky-block or image
     *
     * The GUI is positioned in bottom right corner.
     * Includes collapse/expand functionality to save screen space.
     * 
     * @param controls - OrbitControls for camera manipulation
     * @param camera - PerspectiveCamera to control
     * @param renderFn - Function to call after camera updates
     * @param centerCameraFn - Function to center camera on current model
     * @returns HTML element containing GUI
     */
    createGUI(
        controls: OrbitControls,
        camera: THREE.PerspectiveCamera,
        renderFn: () => void,
        centerCameraFn: () => void
    ): HTMLElement {
        this.cameraActions = createCameraActions(controls, camera, renderFn, centerCameraFn);

        const gui = document.createElement('div');
        gui.className = 'scene-controls-gui';
        // A named group, so a screen reader lists it as the model's controls rather than as a run of
        // unexplained buttons.
        gui.setAttribute('role', 'group');
        gui.setAttribute('aria-label', 'Ovládání modelu');
        gui.style.cssText = `
            position: absolute;
            bottom: 15px;
            right: 0;
            z-index: 1000;
            display: flex;
            flex-direction: row;
            align-items: center;
            gap: 8px;
            background: #1a1a1a;
            padding: 10px;
            border-radius: 6px 0 0 6px;
            user-select: none;
            transition: transform 0.3s ease;
            transform: translateX(0);
        `;

        let isVisible = true;

        const toggleButton = this.createToggleButton(() => {
            isVisible = !isVisible;
            if (isVisible) {
                gui.style.transform = 'translateX(0)';
                toggleButton.textContent = '►';
                toggleButton.title = 'Skrýt ovládání';
                toggleButton.setAttribute('aria-label', 'Skrýt ovládání modelu');
            } else {
                const offset = gui.offsetWidth - 40;
                gui.style.transform = `translateX(${offset}px)`;
                toggleButton.textContent = '◄';
                toggleButton.title = 'Zobrazit ovládání';
                toggleButton.setAttribute('aria-label', 'Zobrazit ovládání modelu');
            }
            toggleButton.setAttribute('aria-expanded', isVisible ? 'true' : 'false');
        });
        gui.appendChild(toggleButton);

        const controlsContainer = this.createControlsContainer();
        const bgContainer = this.createBackgroundContainer();

        const opacityContainer = document.createElement('div');
        opacityContainer.style.cssText = `
            display: flex;
            flex-direction: column;
            gap: 4px;
            background: rgba(255,255,255,0.03);
            padding: 6px;
            border-radius: 4px;
            width: 100%;
            align-items: center;
        `;

        const opacityLabel = document.createElement('label');
        opacityLabel.textContent = 'Průhlednost masky';
        // The id was already set on the input, but htmlFor appeared nowhere in this file, so the label
        // sat beside its field without being attached to it.
        opacityLabel.htmlFor = 'threejs-mask-opacity';
        opacityLabel.style.cssText = `
            font-size: 12px;
            color: white;
            font-weight: 600;
            display: block;
        `;

        const opacityRow = document.createElement('div');
        opacityRow.style.cssText = 'display:flex; width:100%; gap:8px; align-items:center;';

        const opacityInput = document.createElement('input');
        opacityInput.type = 'range';
        opacityInput.min = '0';
        opacityInput.max = '100';
        opacityInput.step = '1';
        opacityInput.value = '50';
        opacityInput.id = 'threejs-mask-opacity';
        opacityInput.style.cssText = 'flex:1;';

        const opacityValue = document.createElement('span');
        opacityValue.textContent = '50%';
        opacityValue.style.cssText = 'width:40px; text-align:right; color: white; font-weight:600;';

        opacityInput.addEventListener('input', () => {
            const val = Number(opacityInput.value);
            opacityValue.textContent = val + '%';
            (window as any).THREEJS_MASK_OPACITY = 1 - (val / 100);
            window.dispatchEvent(new CustomEvent('threejs-mask-opacity-changed', { detail: { opacity: val / 100 } }));
        });

        (window as any).THREEJS_MASK_OPACITY = 0.5;

        opacityRow.appendChild(opacityInput);
        opacityRow.appendChild(opacityValue);
        opacityContainer.appendChild(opacityLabel);
        opacityContainer.appendChild(opacityRow);

        opacityContainer.style.display = 'none';
        bgContainer.style.display = 'none';

        const appearanceToggleButton = document.createElement('button');
        appearanceToggleButton.type = 'button';
        appearanceToggleButton.textContent = 'Zobrazit vzhled';
        appearanceToggleButton.setAttribute('aria-expanded', 'false');
        appearanceToggleButton.style.cssText = `
            background: rgba(255, 255, 255, 0.15);
            border: 1px solid rgba(255, 255, 255, 0.6);
            color: white;
            border-radius: 4px;
            cursor: pointer;
            font-size: 13px;
            font-weight: 600;
            padding: 8px;
            min-height: 44px;
            width: 100%;
        `;

        let appearanceVisible = false;
        appearanceToggleButton.addEventListener('click', () => {
            appearanceVisible = !appearanceVisible;
            opacityContainer.style.display = appearanceVisible ? 'flex' : 'none';
            bgContainer.style.display = appearanceVisible ? 'flex' : 'none';
            appearanceToggleButton.textContent = appearanceVisible ? 'Skrýt vzhled' : 'Zobrazit vzhled';
            appearanceToggleButton.setAttribute('aria-expanded', appearanceVisible ? 'true' : 'false');
        });

        const controlsSection = document.createElement('div');
        controlsSection.style.cssText = `
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 6px;
            width: 100%;
        `;

        controlsSection.appendChild(controlsContainer);

        const controlStack = document.createElement('div');
        controlStack.style.cssText = `
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8px;
        `;

        controlStack.appendChild(controlsSection);
        controlStack.appendChild(this.createKeyboardHint());
        controlStack.appendChild(appearanceToggleButton);
        controlStack.appendChild(opacityContainer);
        controlStack.appendChild(bgContainer);

        gui.appendChild(controlStack);

        this.gui = gui;
        return gui;
    }

    /**
     * @returns the camera operations this panel drives, so the canvas can bind the same ones to keys.
     */
    getCameraActions(): CameraActions | null {
        return this.cameraActions;
    }

    /**
     * Says which keys work once the scene has focus. Without it the keyboard route exists but nobody
     * finds it.
     */
    private createKeyboardHint(): HTMLElement {
        const hint = document.createElement('p');
        hint.className = 'scene-controls-hint';
        hint.textContent = 'Klávesnice: šipky otáčejí, + a − přibližují, R vycentruje';
        hint.style.cssText = `
            margin: 0;
            max-width: 150px;
            font-size: 12px;
            line-height: 1.35;
            color: white;
            text-align: center;
        `;
        return hint;
    }

    /**
     * Create toggle button
     *
     * Creates a button to show/hide the GUI controls panel.
     *
     * @param onToggle - Callback function executed when toggle button is clicked
     * @returns HTMLButtonElement configured as toggle button
     */
    private createToggleButton(onToggle: () => void): HTMLButtonElement {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = '►';
        button.className = 'scene-controls-toggle';
        button.setAttribute('aria-label', 'Skrýt ovládání modelu');
        button.setAttribute('aria-expanded', 'true');
        button.style.cssText = `
            background: transparent;
            border: none;
            color: white;
            cursor: pointer;
            font-size: 20px;
            font-weight: bold;
            width: 44px;
            height: 44px;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: transform 0.2s;
            padding: 0;
            flex-shrink: 0;
        `;

        button.addEventListener('mouseenter', () => {
            button.style.transform = 'scale(1.2)';
        });

        button.addEventListener('mouseleave', () => {
            button.style.transform = 'scale(1)';
        });

        button.addEventListener('click', onToggle);
        button.title = 'Skrýt ovládání';

        return button;
    }

    /**
     * Create direction controls container
     *
     * Creates a 3x3 grid with rotation controls (up/down/left/right), zoom and the centring button.
     * Every button drives the shared camera operations, the same ones the keyboard uses.
     *
     * @returns HTML element containing direction control buttons
     */
    private createControlsContainer(): HTMLElement {
        const actions = this.cameraActions!;
        const size = GUIManager.BUTTON_SIZE;
        const gap = GUIManager.GRID_GAP;
        const total = size * 3 + gap * 2;

        const container = document.createElement('div');
        container.style.cssText = `
            display: grid;
            grid-template-columns: repeat(3, ${size}px);
            grid-template-rows: repeat(3, ${size}px);
            gap: ${gap}px;
            width: ${total}px;
            height: ${total}px;
        `;

        const buttons = [
            this.createControlButton('▲', '1 / 2 / 2 / 3', 'Otočit nahoru', actions.rotateUp),
            this.createControlButton('▼', '3 / 2 / 4 / 3', 'Otočit dolů', actions.rotateDown),
            this.createControlButton('◄', '2 / 1 / 3 / 2', 'Otočit vlevo', actions.rotateLeft),
            this.createControlButton('►', '2 / 3 / 3 / 4', 'Otočit vpravo', actions.rotateRight),
            this.createControlButton('⟲', '2 / 2 / 3 / 3', 'Vycentrovat kameru na model', actions.reset, true),
            this.createControlButton('+', '3 / 3 / 4 / 4', 'Přiblížit', actions.zoomIn),
            this.createControlButton('−', '3 / 1 / 4 / 2', 'Oddálit', actions.zoomOut)
        ];

        buttons.forEach((button) => container.appendChild(button));

        return container;
    }

    /**
     * Create background controls container
     *
     * Creates controls for selecting background type (color/image/sky-block)
     * and adjusting zoom level and model fit.
     *
     * @returns HTML element containing background control inputs
     */
    private createBackgroundContainer(): HTMLElement {
        const container = document.createElement('div');
        container.style.cssText = `
            display: flex;
            flex-direction: column;
            gap: 4px;
            background: rgba(255,255,255,0.03);
            padding: 6px;
            border-radius: 4px;
            width: 100%;
        `;

        // Label for the background controls, now actually attached to the select it names.
        const title = document.createElement('label');
        title.textContent = 'Pozadí';
        title.htmlFor = 'threejs-bg-select';
        title.style.cssText = `
            font-size: 12px;
            color: white;
            font-weight: 600;
            text-align: left;
            display: block;
        `;

        const bgSelect = document.createElement('select');
        bgSelect.id = 'threejs-bg-select';
        const options = [
            { v: 'cube', t: 'Obloha' },
            { v: 'color', t: 'Barva' },
            { v: 'image', t: 'Obrázek' }
        ];
        options.forEach(o => {
            const opt = document.createElement('option');
            opt.value = o.v;
            opt.textContent = o.t;
            bgSelect.appendChild(opt);
        });
        bgSelect.style.fontSize = '16px';

        // Neither of the two inputs below had an id or a label of its own: a colour picker and a file
        // chooser announced as nothing but their type.
        const colorLabel = document.createElement('label');
        colorLabel.textContent = 'Barva pozadí';
        colorLabel.htmlFor = 'threejs-bg-color';
        colorLabel.style.cssText = 'font-size: 12px; color: white; font-weight: 600; display: block;';

        const colorInput = document.createElement('input');
        colorInput.type = 'color';
        colorInput.id = 'threejs-bg-color';
        colorInput.value = '#000000';
        colorInput.style.cssText = 'width: 100%; min-height: 44px;';

        const fileLabel = document.createElement('label');
        fileLabel.textContent = 'Obrázek pozadí';
        fileLabel.htmlFor = 'threejs-bg-image';
        fileLabel.style.cssText = 'font-size: 12px; color: white; font-weight: 600; display: block;';

        const toHexColor = (value: unknown): string | null => {
            if (typeof value === 'string') {
                const v = value.trim();
                if (/^#[0-9a-fA-F]{6}$/.test(v)) return v;
                if (/^#[0-9a-fA-F]{3}$/.test(v)) {
                    const r = v[1], g = v[2], b = v[3];
                    return `#${r}${r}${g}${g}${b}${b}`;
                }
                if (/^0x[0-9a-fA-F]{6}$/.test(v)) return `#${v.slice(2)}`;
                if (/^[0-9a-fA-F]{6}$/.test(v)) return `#${v}`;
                return null;
            }

            if (typeof value === 'number' && Number.isFinite(value)) {
                return `#${Math.max(0, Math.min(0xffffff, value)).toString(16).padStart(6, '0')}`;
            }

            return null;
        };

        const dispatchColorBackground = () => {
            window.dispatchEvent(new CustomEvent('threejs-set-background', {
                detail: { type: 'color', value: colorInput.value }
            }));
        };

        colorInput.addEventListener('input', () => {
            dispatchColorBackground();
        });

        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.id = 'threejs-bg-image';
        fileInput.accept = 'image/*';
        fileInput.addEventListener('change', async (ev) => {
            const f = (ev.target as HTMLInputElement).files?.[0];
            if (!f) return;
            const reader = new FileReader();
            reader.onload = () => {
                const dataUrl = reader.result as string;
                const event = new CustomEvent('threejs-set-background', { detail: { type: 'image', value: dataUrl } });
                window.dispatchEvent(event);
            };
            reader.readAsDataURL(f);
        });

        container.appendChild(title);
        container.appendChild(bgSelect);
        container.appendChild(colorLabel);
        container.appendChild(colorInput);
        container.appendChild(fileLabel);
        container.appendChild(fileInput);

        // A label has to be hidden together with its field, or it names something that is not there.
        const syncInputVisibility = () => {
            const showColor = bgSelect.value === 'color';
            const showImage = bgSelect.value === 'image';
            colorInput.style.display = showColor ? 'block' : 'none';
            colorLabel.style.display = showColor ? 'block' : 'none';
            fileInput.style.display = showImage ? 'block' : 'none';
            fileLabel.style.display = showImage ? 'block' : 'none';
        };
        syncInputVisibility();

         if (this.backgroundSyncHandler) {
             window.removeEventListener('threejs-background-updated', this.backgroundSyncHandler);
         }
         this.backgroundSyncHandler = (ev: Event) => {
             const customEv = ev as CustomEvent;
             const bg = customEv.detail;
             if (!bg || typeof bg !== 'object') {
                 return;
             }

             if (bg.type === 'cube' || bg.type === 'color' || bg.type === 'image') {
                 bgSelect.value = bg.type;
             }
             if (bg.type === 'color') {
                 const normalizedColor = toHexColor(bg.value);
                 if (normalizedColor) {
                     colorInput.value = normalizedColor;
                 }
              }

             syncInputVisibility();
         };
         window.addEventListener('threejs-background-updated', this.backgroundSyncHandler);

         bgSelect.addEventListener('change', () => {
              const v = bgSelect.value;
              syncInputVisibility();
              if (v === 'cube') {
                  const event = new CustomEvent('threejs-set-background', { detail: { type: 'cube', value: { files: ['px.bmp','nx.bmp','py.bmp','ny.bmp','pz.bmp','nz.bmp'], path: 'skybox/' } } });
                  window.dispatchEvent(event);
                  return;
              }

              if (v === 'color') {
                 // Apply currently selected color immediately when switching back from skybox/image.
                 dispatchColorBackground();
              }
          });

        return container;
    }

    /**
     * Create control button
     *
     * One step per click, whether the click came from a mouse or from Enter or the space bar, and a run
     * of steps while the button is held down — with a pointer or with a key.
     *
     * The arrows and magnifiers previously listened only for mousedown, mouseup and mouseleave, so
     * Enter and the space bar did nothing on them and the scene could not be turned from a keyboard.
     *
     * @param text - Button glyph
     * @param gridArea - CSS grid-area value for positioning in grid layout
     * @param label - Accessible name; a glyph in textContent is not one
     * @param action - Camera operation to run
     * @param singleStepOnly - true for the centring button, which has nothing to repeat
     * @returns HTMLButtonElement configured with specified behavior
     */
    private createControlButton(
        text: string,
        gridArea: string,
        label: string,
        action: () => void,
        singleStepOnly: boolean = false
    ): HTMLButtonElement {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = text;
        button.title = label;
        button.setAttribute('aria-label', label);
        button.style.cssText = `
            grid-area: ${gridArea};
            background: rgba(255, 255, 255, 0.2);
            border: 2px solid rgba(255, 255, 255, 0.85);
            color: white;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            font-weight: bold;
            transition: background 0.2s;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100%;
            width: 100%;
        `;

        button.addEventListener('mouseenter', () => {
            button.style.background = 'rgba(255, 255, 255, 0.3)';
        });

        button.addEventListener('mouseleave', () => {
            button.style.background = 'rgba(255, 255, 255, 0.2)';
        });

        // Fires for a mouse click and for Enter or the space bar alike, so one code path covers both.
        button.addEventListener('click', action);

        if (singleStepOnly) {
            return button;
        }

        const startRepeat = () => {
            this.stopRepeat();
            this.intervalId = window.setInterval(action, 50);
        };

        button.addEventListener('pointerdown', () => {
            button.style.background = 'rgba(255, 255, 255, 0.4)';
            startRepeat();
        });
        button.addEventListener('pointerup', () => this.stopRepeat());
        button.addEventListener('pointerleave', () => this.stopRepeat());
        button.addEventListener('pointercancel', () => this.stopRepeat());

        // Holding a key repeats it through the browser's own auto-repeat; the default action is
        // suppressed so the synthesised click does not double every step.
        button.addEventListener('keydown', (event: KeyboardEvent) => {
            if (event.key !== 'Enter' && event.key !== ' ') {
                return;
            }
            event.preventDefault();
            action();
        });
        button.addEventListener('blur', () => this.stopRepeat());

        return button;
    }

    private stopRepeat(): void {
        if (this.intervalId !== null) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }
    }

    /**
     * Attach GUI to canvas
     *
     * Appends the GUI controls to the parent element of the canvas.
     * Ensures parent has relative positioning for proper GUI placement.
     *
     * @param canvasElement - HTML canvas element or its container to attach GUI to
     */
    attachToCanvas(canvasElement: HTMLElement): void {
        if (!this.gui) return;

        const parent = canvasElement.parentElement;
        if (parent) {
            const parentStyle = window.getComputedStyle(parent);
            if (parentStyle.position === 'static') {
                parent.style.position = 'relative';
            }
            parent.appendChild(this.gui);
        }
    }

    /**
     * Remove GUI
     */
    dispose(): void {
        if (this.intervalId !== null) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }

        if (this.gui && this.gui.parentElement) {
            this.gui.parentElement.removeChild(this.gui);
            this.gui = null;
        }

        if (this.backgroundSyncHandler) {
            window.removeEventListener('threejs-background-updated', this.backgroundSyncHandler);
            this.backgroundSyncHandler = null;
        }
    }
}
