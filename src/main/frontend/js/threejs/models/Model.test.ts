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

  it('resets texture transform defaults when new model has no reference UV map', () => {
    const model = new Model('model-1', '/models/model-a.glb');
    const texture = new THREE.Texture();
    texture.flipY = false;
    texture.wrapS = THREE.RepeatWrapping;
    texture.wrapT = THREE.RepeatWrapping;
    texture.repeat.set(2, 3);
    texture.offset.set(0.25, 0.5);
    texture.rotation = 0.33;
    texture.center.set(0.5, 0.5);

    const referenceMap = new THREE.Texture();
    referenceMap.flipY = false;
    const mappedMaterial = new THREE.MeshStandardMaterial({map: referenceMap});
    const firstMesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), mappedMaterial);
    const firstGroup = new THREE.Group();
    firstGroup.add(firstMesh);

    model.modelLoader = firstGroup;
    model.applyTexture(texture);
    expect(texture.flipY).toBe(false);

    const plainMaterial = new THREE.MeshStandardMaterial();
    const secondMesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), plainMaterial);
    const secondGroup = new THREE.Group();
    secondGroup.add(secondMesh);

    model.modelLoader = secondGroup;
    model.applyTexture(texture);

    expect(texture.flipY).toBe(true);
    expect(texture.wrapS).toBe(THREE.ClampToEdgeWrapping);
    expect(texture.wrapT).toBe(THREE.ClampToEdgeWrapping);
    expect(texture.repeat.x).toBe(1);
    expect(texture.repeat.y).toBe(1);
    expect(texture.offset.x).toBe(0);
    expect(texture.offset.y).toBe(0);
    expect(texture.rotation).toBe(0);
    expect(texture.center.x).toBe(0);
    expect(texture.center.y).toBe(0);
  });

  it('restores original model materials after texture override is removed', () => {
    const model = new Model('model-1', '/models/femur.glb');
    const originalColor = new THREE.Color(0x123456);
    const originalMaterial = new THREE.MeshStandardMaterial({color: originalColor});
    const mesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), originalMaterial);
    const group = new THREE.Group();
    group.add(mesh);
    model.modelLoader = group;

    const overlayTexture = new THREE.Texture();
    model.applyTexture(overlayTexture);

    const overriddenMaterial = mesh.material as THREE.MeshStandardMaterial;
    expect(overriddenMaterial.map).toBe(overlayTexture);

    model.restoreBaseMaterials();

    const restoredMaterial = mesh.material as THREE.MeshStandardMaterial;
    expect(restoredMaterial.map ?? null).toBeNull();
    expect(restoredMaterial.color.getHex()).toBe(0x123456);
  });
});
