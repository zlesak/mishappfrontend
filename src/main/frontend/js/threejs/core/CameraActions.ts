import * as THREE from 'three';
import type { OrbitControls } from 'three/addons';

/**
 * The camera movements the viewer offers, as named operations.
 *
 * Extracted so that the control panel and the keyboard drive exactly the same code. They used to be
 * unrelated: the panel's arrows were wired to mouse events only, and the scene had no key handling at
 * all — the string "keydown" did not appear anywhere in the Three.js layer — so a keyboard user could
 * neither turn the model nor zoom it.
 */
export interface CameraActions {
    rotateUp(): void;
    rotateDown(): void;
    rotateLeft(): void;
    rotateRight(): void;
    zoomIn(): void;
    zoomOut(): void;
    reset(): void;
}

/** How far one step turns the camera, in radians. */
const ROTATE_STEP = 0.1;

/** How far one step moves the camera along its line of sight, in scene units. */
const ZOOM_STEP = 0.5;

/**
 * Builds the camera operations for one scene.
 *
 * @param controls - OrbitControls holding the orbit target
 * @param camera - the camera to move
 * @param renderFn - called after every change so the frame is redrawn
 * @param centerCameraFn - restores the default view of the current model
 * @returns the operations, each of which redraws
 */
export function createCameraActions(
    controls: OrbitControls,
    camera: THREE.PerspectiveCamera,
    renderFn: () => void,
    centerCameraFn: () => void
): CameraActions {
    const orbit = (mutate: (spherical: THREE.Spherical) => void): void => {
        const spherical = new THREE.Spherical();
        const offset = new THREE.Vector3();
        offset.copy(camera.position).sub(controls.target);
        spherical.setFromVector3(offset);
        mutate(spherical);
        offset.setFromSpherical(spherical);
        camera.position.copy(controls.target).add(offset);
        controls.update();
        renderFn();
    };

    const dolly = (distance: number): void => {
        const direction = new THREE.Vector3();
        direction.subVectors(controls.target, camera.position).normalize();
        camera.position.add(direction.multiplyScalar(distance));
        controls.update();
        renderFn();
    };

    // The polar angle is clamped short of the poles: at exactly 0 or PI the orbit loses its up vector
    // and the view flips.
    const clampPhi = (phi: number): number => Math.max(0.1, Math.min(Math.PI - 0.1, phi));

    return {
        rotateUp: () => orbit((s) => {
            s.phi = clampPhi(s.phi - ROTATE_STEP);
        }),
        rotateDown: () => orbit((s) => {
            s.phi = clampPhi(s.phi + ROTATE_STEP);
        }),
        rotateLeft: () => orbit((s) => {
            s.theta -= ROTATE_STEP;
        }),
        rotateRight: () => orbit((s) => {
            s.theta += ROTATE_STEP;
        }),
        zoomIn: () => dolly(ZOOM_STEP),
        zoomOut: () => dolly(-ZOOM_STEP),
        reset: () => {
            centerCameraFn();
            renderFn();
        }
    };
}

/**
 * Binds the keyboard to the camera operations.
 *
 * Arrows turn the model, plus and minus zoom, R restores the default view — the same seven operations
 * the control panel offers, so neither route can drift from the other.
 *
 * @param target - the canvas, which must be focusable for any of this to be reachable
 * @param actions - the operations to drive
 * @returns a function that removes the binding
 */
export function bindKeyboardControls(target: HTMLElement, actions: CameraActions): () => void {
    const handler = (event: KeyboardEvent): void => {
        if (event.ctrlKey || event.metaKey || event.altKey) {
            return;
        }

        switch (event.key) {
            case 'ArrowUp':
                actions.rotateUp();
                break;
            case 'ArrowDown':
                actions.rotateDown();
                break;
            case 'ArrowLeft':
                actions.rotateLeft();
                break;
            case 'ArrowRight':
                actions.rotateRight();
                break;
            case '+':
            case '=':
                actions.zoomIn();
                break;
            case '-':
            case '_':
                actions.zoomOut();
                break;
            case 'r':
            case 'R':
                actions.reset();
                break;
            default:
                return;
        }

        // Only once a key has been recognised: otherwise Tab would be swallowed and the canvas would
        // become a trap the user could not leave.
        event.preventDefault();
    };

    target.addEventListener('keydown', handler);
    return () => target.removeEventListener('keydown', handler);
}
