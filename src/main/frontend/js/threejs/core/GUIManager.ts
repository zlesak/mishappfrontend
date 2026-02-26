import * as THREE from 'three';
import type { OrbitControls } from 'three/addons';

/**
 * GUI controls for interactive camera manipulation
 * 
 * Provides buttons for common camera operations:
 * - Rotate left/right
 * - Pan up/down/left/right
 * - Zoom in/out
 * - Reset camera
 * - Show/hide GUI
 *
 * Controls use OrbitControls for smooth camera manipulation and trigger renders after each update.
 */
export class GUIManager {
    private gui: HTMLElement | null = null;
    private intervalId: number | null = null;

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
        const gui = document.createElement('div');
        gui.className = 'scene-controls-gui';
        gui.style.cssText = `
            position: absolute;
            bottom: 15px;
            right: 0;
            z-index: 1000;
            display: flex;
            flex-direction: row;
            align-items: center;
            gap: 8px;
            background: rgba(0, 0, 0, 0.7);
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
            } else {
                const offset = gui.offsetWidth - 40;
                gui.style.transform = `translateX(${offset}px)`;
                toggleButton.textContent = '◄';
                toggleButton.title = 'Zobrazit ovládání';
            }
        });
        gui.appendChild(toggleButton);

        const controlsContainer = this.createControlsContainer(controls, camera, renderFn, centerCameraFn);
        const zoomContainer = this.createZoomContainer(controls, camera, renderFn);
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

        const controlStack = document.createElement('div');
        controlStack.style.cssText = `
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8px;
        `;

        controlStack.appendChild(controlsContainer);
        controlStack.appendChild(zoomContainer);
        controlStack.appendChild(opacityContainer);
        controlStack.appendChild(bgContainer);

        gui.appendChild(controlStack);

        this.gui = gui;
        return gui;
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
        button.textContent = '►';
        button.className = 'scene-controls-toggle';
        button.style.cssText = `
            background: transparent;
            border: none;
            color: white;
            cursor: pointer;
            font-size: 16px;
            font-weight: bold;
            width: 20px;
            height: 20px;
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
     * Creates a 3x3 grid with rotation controls (up/down/left/right) and reset button.
     *
     * @param controls - OrbitControls for camera manipulation
     * @param camera - PerspectiveCamera to control
     * @param renderFn - Function to call after camera updates
     * @param centerCameraFn - Function to center camera on current model
     * @returns HTML element containing direction control buttons
     */
    private createControlsContainer(
        controls: OrbitControls,
        camera: THREE.PerspectiveCamera,
        renderFn: () => void,
        centerCameraFn: () => void
    ): HTMLElement {
        const container = document.createElement('div');
        container.style.cssText = `
            display: grid;
            grid-template-columns: repeat(3, 35px);
            grid-template-rows: repeat(3, 35px);
            gap: 4px;
            width: 100%;
        `;

        const rotateSpeed = 0.1;

        const upButton = this.createControlButton('▲', '1 / 2 / 2 / 3', () => {
            const spherical = new THREE.Spherical();
            const offset = new THREE.Vector3();
            offset.copy(camera.position).sub(controls.target);
            spherical.setFromVector3(offset);
            spherical.phi = Math.max(0.1, Math.min(Math.PI - 0.1, spherical.phi - rotateSpeed));
            offset.setFromSpherical(spherical);
            camera.position.copy(controls.target).add(offset);
            controls.update();
        }, renderFn);

        const downButton = this.createControlButton('▼', '3 / 2 / 4 / 3', () => {
            const spherical = new THREE.Spherical();
            const offset = new THREE.Vector3();
            offset.copy(camera.position).sub(controls.target);
            spherical.setFromVector3(offset);
            spherical.phi = Math.max(0.1, Math.min(Math.PI - 0.1, spherical.phi + rotateSpeed));
            offset.setFromSpherical(spherical);
            camera.position.copy(controls.target).add(offset);
            controls.update();
        }, renderFn);

        const leftButton = this.createControlButton('◄', '2 / 1 / 3 / 2', () => {
            const spherical = new THREE.Spherical();
            const offset = new THREE.Vector3();
            offset.copy(camera.position).sub(controls.target);
            spherical.setFromVector3(offset);
            spherical.theta -= rotateSpeed;
            offset.setFromSpherical(spherical);
            camera.position.copy(controls.target).add(offset);
            controls.update();
        }, renderFn);

        const rightButton = this.createControlButton('►', '2 / 3 / 3 / 4', () => {
            const spherical = new THREE.Spherical();
            const offset = new THREE.Vector3();
            offset.copy(camera.position).sub(controls.target);
            spherical.setFromVector3(offset);
            spherical.theta += rotateSpeed;
            offset.setFromSpherical(spherical);
            camera.position.copy(controls.target).add(offset);
            controls.update();
        }, renderFn);

        const resetButton = this.createControlButton('⟲', '2 / 2 / 3 / 3', () => {
            centerCameraFn();
        }, renderFn, true);
        resetButton.title = 'Vycentrovat kameru na model';

        container.appendChild(upButton);
        container.appendChild(downButton);
        container.appendChild(leftButton);
        container.appendChild(rightButton);
        container.appendChild(resetButton);

        return container;
    }

    /**
     * Create zoom controls container
     *
     * Creates vertical buttons for zoom in/out functionality.
     *
     * @param controls - OrbitControls for camera manipulation
     * @param camera - PerspectiveCamera to control
     * @param renderFn - Function to call after camera updates
     * @returns HTML element containing zoom control buttons
     */
    private createZoomContainer(
        controls: OrbitControls,
        camera: THREE.PerspectiveCamera,
        renderFn: () => void
    ): HTMLElement {
        const container = document.createElement('div');
        container.style.cssText = `
            display: flex;
            flex-direction: row;
            gap: 4px;
            width: 100%;
        `;

        const moveSpeed = 0.5;

        const zoomInButton = this.createControlButton('+', 'auto', () => {
            const direction = new THREE.Vector3();
            direction.subVectors(controls.target, camera.position).normalize();
            camera.position.add(direction.multiplyScalar(moveSpeed));
            controls.update();
        }, renderFn);
        zoomInButton.style.width = '50%';
        zoomInButton.style.height = '35px';
        zoomInButton.style.fontSize = '16px';

        const zoomOutButton = this.createControlButton('−', 'auto', () => {
            const direction = new THREE.Vector3();
            direction.subVectors(controls.target, camera.position).normalize();
            camera.position.sub(direction.multiplyScalar(moveSpeed));
            controls.update();
        }, renderFn);
        zoomOutButton.style.width = '50%';
        zoomOutButton.style.height = '35px';
        zoomOutButton.style.fontSize = '16px';

        container.appendChild(zoomInButton);
        container.appendChild(zoomOutButton);

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

        // Label for the background controls
        const title = document.createElement('label');
        title.textContent = 'Pozadí';
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

        const colorInput = document.createElement('input');
        colorInput.type = 'color';
        colorInput.value = '#000000';
        colorInput.style.width = '100%';
        colorInput.addEventListener('input', () => {
            const color = colorInput.value;
            const event = new CustomEvent('threejs-set-background', { detail: { type: 'color', value: color } });
            window.dispatchEvent(event);
        });

        const fileInput = document.createElement('input');
        fileInput.type = 'file';
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
        container.appendChild(colorInput);
        container.appendChild(fileInput);

        colorInput.style.display = bgSelect.value === 'color' ? 'block' : 'none';
        fileInput.style.display = bgSelect.value === 'image' ? 'block' : 'none';

         bgSelect.addEventListener('change', () => {
             const v = bgSelect.value;
             colorInput.style.display = v === 'color' ? 'block' : 'none';
             fileInput.style.display = v === 'image' ? 'block' : 'none';
             if (v === 'cube') {
                 const event = new CustomEvent('threejs-set-background', { detail: { type: 'cube', value: { files: ['px.bmp','nx.bmp','py.bmp','ny.bmp','pz.bmp','nz.bmp'], path: 'skybox/' } } });
                 window.dispatchEvent(event);
             }
         });

        return container;
    }

    /**
     * Create control button
     *
     * Creates a styled button with continuous action on mouse hold (except reset button).
     * Supports both single-click and hold-to-repeat functionality.
     *
     * @param text - Button label text
     * @param gridArea - CSS grid-area value for positioning in grid layout
     * @param action - Function to execute on button activation
     * @param renderFn - Function to call after action to re-render scene
     * @param isResetButton - If true, button only fires once per click; if false, repeats while held (default: false)
     * @returns HTMLButtonElement configured with specified behavior
     */
    private createControlButton(
        text: string,
        gridArea: string,
        action: () => void,
        renderFn: () => void,
        isResetButton: boolean = false
    ): HTMLButtonElement {
        const button = document.createElement('button');
        button.textContent = text;
        button.style.cssText = `
            grid-area: ${gridArea};
            background: rgba(255, 255, 255, 0.2);
            border: 1px solid rgba(255, 255, 255, 0.3);
            color: white;
            border-radius: 3px;
            cursor: pointer;
            font-size: ${isResetButton ? '16px' : '14px'};
            font-weight: bold;
            transition: background 0.2s;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 35px;
            width: 35px;
        `;

        button.addEventListener('mouseenter', () => {
            button.style.background = 'rgba(255, 255, 255, 0.3)';
        });

        button.addEventListener('mouseleave', () => {
            button.style.background = 'rgba(255, 255, 255, 0.2)';
        });

        if (isResetButton) {
            button.addEventListener('click', () => {
                action();
                renderFn();
            });
        } else {
            button.addEventListener('mousedown', () => {
                button.style.background = 'rgba(255, 255, 255, 0.4)';
                action();
                renderFn();
                this.intervalId = window.setInterval(() => {
                    action();
                    renderFn();
                }, 50);
            });

            const stopAction = () => {
                button.style.background = 'rgba(255, 255, 255, 0.3)';
                if (this.intervalId !== null) {
                    clearInterval(this.intervalId);
                    this.intervalId = null;
                }
            };

            button.addEventListener('mouseup', stopAction);
            button.addEventListener('mouseleave', stopAction);
        }

        return button;
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
    }
}
