import * as THREE from 'three';
import {describe, expect, it, vi} from 'vitest';
import {Model} from './Model';

describe('Model', () => {
  it('manages linked questions without duplicates', () => {
    const model = new Model('model-1', '/models/femur.glb', true, 'q-1');

    model.addQuestion('q-1');
    model.addQuestion('q-2');
    model.removeQuestion('q-1');

    expect(model.questions).toEqual(['q-2']);
    expect(model.question).toBeNull();
    expect(model.hasQuestions()).toBe(true);
  });

  it('manages main and additional textures including disposal', () => {
    const model = new Model('model-1', '/models/femur.glb');
    const mainTexture = new THREE.Texture();
    const firstOther = new THREE.Texture();
    const secondOther = new THREE.Texture();
    const mainDispose = vi.spyOn(mainTexture, 'dispose');
    const firstOtherDispose = vi.spyOn(firstOther, 'dispose');
    const secondOtherDispose = vi.spyOn(secondOther, 'dispose');

    model.setMainTexture('/textures/main.png', mainTexture);
    model.addOtherTexture('other-1', firstOther);
    model.addOtherTexture('other-1', firstOther);
    model.addOtherTexture('other-2', secondOther);
    model.removeOtherTexture('other-1');
    model.clearMainTexture();
    model.clearOtherTextures();

    expect(mainDispose).toHaveBeenCalledTimes(1);
    expect(firstOtherDispose).toHaveBeenCalledTimes(1);
    expect(secondOtherDispose).toHaveBeenCalledTimes(1);
    expect(model.mainTexture).toBeNull();
    expect(model.loadedMainTexture).toBeNull();
    expect(model.otherTextures).toEqual([]);
  });

  it('applies texture to mesh materials and disposes old resources on dispose', () => {
    const model = new Model('model-1', '/models/femur.glb');
    const group = new THREE.Group();
    const geometry = new THREE.BoxGeometry(1, 1, 1);
    const oldMaterial = new THREE.MeshBasicMaterial();
    const oldMaterialDispose = vi.spyOn(oldMaterial, 'dispose');
    const mesh = new THREE.Mesh(geometry, oldMaterial);
    const appliedTexture = new THREE.Texture();
    const loaderTexture = new THREE.Texture();
    const loaderDispose = vi.spyOn(loaderTexture, 'dispose');

    group.add(mesh);
    model.modelLoader = group;
    model.textureLoader = loaderTexture;

    model.applyTexture(appliedTexture);

    expect(oldMaterialDispose).toHaveBeenCalledTimes(1);
    expect(mesh.material).toBeInstanceOf(THREE.MeshStandardMaterial);
    expect(((mesh.material as unknown) as THREE.MeshStandardMaterial).map).toBe(appliedTexture);

    model.dispose();

    expect(loaderDispose).toHaveBeenCalledTimes(1);
    expect(model.modelLoader).toBeNull();
    expect(model.textureLoader).toBeNull();
  });
});
