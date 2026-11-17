import {describe, expect, it, vi} from 'vitest';
import * as THREE from 'three';
import {bindKeyboardControls, createCameraActions, type CameraActions} from './CameraActions';

function scene() {
    const camera = new THREE.PerspectiveCamera();
    camera.position.set(0, 0, 4);
    const controls = {target: new THREE.Vector3(0, 0, 0), update: vi.fn()} as unknown as never;
    const render = vi.fn();
    const center = vi.fn();
    return {camera, controls, render, center, actions: createCameraActions(controls, camera, render, center)};
}

function stubActions(): CameraActions & {calls: string[]} {
    const calls: string[] = [];
    return {
        calls,
        rotateUp: () => calls.push('up'),
        rotateDown: () => calls.push('down'),
        rotateLeft: () => calls.push('left'),
        rotateRight: () => calls.push('right'),
        zoomIn: () => calls.push('in'),
        zoomOut: () => calls.push('out'),
        reset: () => calls.push('reset')
    };
}

describe('createCameraActions', () => {
    it('turns the camera around the target and redraws', () => {
        const {camera, actions, render, controls} = scene();
        const before = camera.position.clone();

        actions.rotateLeft();

        expect(camera.position.equals(before)).toBe(false);
        expect(render).toHaveBeenCalled();
        expect((controls as unknown as {update: () => void}).update).toHaveBeenCalled();
        // Orbiting keeps the distance: it turns the view, it does not move away from the model.
        expect(camera.position.length()).toBeCloseTo(before.length(), 5);
    });

    it('never lets the camera reach the pole, where the view would flip', () => {
        const {camera, actions} = scene();
        camera.position.set(0, 4, 0);

        for (let i = 0; i < 100; i++) {
            actions.rotateUp();
        }

        // The clamp is 0.1; the round trip through spherical coordinates costs a few digits.
        const spherical = new THREE.Spherical().setFromVector3(camera.position.clone());
        expect(spherical.phi).toBeCloseTo(0.1, 6);
    });

    it('zooming moves along the line of sight in both directions', () => {
        const {camera, actions} = scene();
        const start = camera.position.length();

        actions.zoomIn();
        const closer = camera.position.length();
        expect(closer).toBeLessThan(start);

        actions.zoomOut();
        expect(camera.position.length()).toBeCloseTo(start, 5);
    });

    it('resetting delegates to the centring function', () => {
        const {actions, center, render} = scene();

        actions.reset();

        expect(center).toHaveBeenCalledTimes(1);
        expect(render).toHaveBeenCalled();
    });
});

describe('bindKeyboardControls', () => {
    it('drives every operation the panel offers', () => {
        const canvas = document.createElement('canvas');
        const actions = stubActions();
        bindKeyboardControls(canvas, actions);

        ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', '+', '-', 'r'].forEach((key) => {
            canvas.dispatchEvent(new KeyboardEvent('keydown', {key, cancelable: true}));
        });

        expect(actions.calls).toEqual(['up', 'down', 'left', 'right', 'in', 'out', 'reset']);
    });

    it('leaves Tab alone so the scene is not a trap', () => {
        const canvas = document.createElement('canvas');
        const actions = stubActions();
        bindKeyboardControls(canvas, actions);

        const tab = new KeyboardEvent('keydown', {key: 'Tab', cancelable: true});
        canvas.dispatchEvent(tab);

        expect(actions.calls).toEqual([]);
        expect(tab.defaultPrevented).toBe(false);
    });

    it('ignores shortcuts held with a modifier, which belong to the browser', () => {
        const canvas = document.createElement('canvas');
        const actions = stubActions();
        bindKeyboardControls(canvas, actions);

        canvas.dispatchEvent(new KeyboardEvent('keydown', {key: '+', ctrlKey: true, cancelable: true}));

        expect(actions.calls).toEqual([]);
    });

    it('stops listening once unbound', () => {
        const canvas = document.createElement('canvas');
        const actions = stubActions();
        const unbind = bindKeyboardControls(canvas, actions);

        unbind();
        canvas.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowUp', cancelable: true}));

        expect(actions.calls).toEqual([]);
    });
});
