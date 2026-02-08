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
        gui.appendChild(controlsContainer);

        const zoomContainer = this.createZoomContainer(controls, camera, renderFn);
        gui.appendChild(zoomContainer);

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
            font-size: 12px;
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
            flex-direction: column;
            gap: 4px;
        `;

        const moveSpeed = 0.5;

        const zoomInButton = this.createControlButton('+', 'auto', () => {
            const direction = new THREE.Vector3();
            direction.subVectors(controls.target, camera.position).normalize();
            camera.position.add(direction.multiplyScalar(moveSpeed));
            controls.update();
        }, renderFn);
        zoomInButton.style.width = '35px';
        zoomInButton.style.height = '53px';
        zoomInButton.style.fontSize = '16px';

        const zoomOutButton = this.createControlButton('−', 'auto', () => {
            const direction = new THREE.Vector3();
            direction.subVectors(controls.target, camera.position).normalize();
            camera.position.sub(direction.multiplyScalar(moveSpeed));
            controls.update();
        }, renderFn);
        zoomOutButton.style.width = '35px';
        zoomOutButton.style.height = '53px';
        zoomOutButton.style.fontSize = '16px';

        container.appendChild(zoomInButton);
        container.appendChild(zoomOutButton);

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
