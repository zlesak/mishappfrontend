import * as THREE from 'three';

/**
 * Model data structure
 */
export interface IModelData {
    id: string;
    advanced: boolean;
    model: string;
    mainTexture: string | null;
    otherTextures: ITextureData[];
    questions: string[];
    question: string | null;
    modelLoader: THREE.Group | THREE.Object3D | null;
    textureLoader: THREE.Texture | null;
    loadedMainTexture: THREE.Texture | null;
    main: boolean;
}

/**
 * Texture data structure
 */
export interface ITextureData {
    textureId: string;
    texture: THREE.Texture;
}

/**
 * Authentication headers
 */
export interface IAuthHeaders {
    [header: string]: string;
}

/**
 * RGB color structure
 */
export interface IRGB {
    r: number;
    g: number;
    b: number;
}

/**
 * Mask application result
 */
export interface IMaskResult {
    model: IModelData;
    lastSelectedTextureId: string | null;
    maskCenter?: THREE.Vector3 | null;
}

/**
 * Model switch result
 */
export interface IModelSwitchResult {
    model: IModelData;
    lastSelectedTextureId: string | null;
}

/**
 * Vaadin server interface for communication
 */
export interface IVaadinServer {
    doingActions?: (description: string) => void;
    finishedActions?: () => void;
    getToken?: () => Promise<string>;
    onColorPicked?: (modelId: string, textureId: string | null, color: string, questionId: string | null) => void;
}

/**
 * Vaadin element interface
 */
export interface IVaadinElement extends HTMLCanvasElement {
    $server?: IVaadinServer;
}

/**
 * Camera animation state
 */
export interface ICameraAnimation {
    active: boolean;
    start: number | null;
    duration: number;
    startPos: THREE.Vector3 | null;
    targetPos: THREE.Vector3 | null;
    controlsStartTarget: THREE.Vector3 | null;
    controlsTargetTarget: THREE.Vector3 | null;
}

/**
 * Scene configuration
 */
export interface ISceneConfig {
    cameraFov?: number;
    cameraAspect?: number;
    cameraNear?: number;
    cameraFar?: number;
    enableDebug?: boolean;
}

/**
 * Resize callback type
 */
export type ResizeCallback = () => void;

/**
 * Click handler callback type
 */
export type ClickHandlerCallback = (event: MouseEvent) => void;

/**
 * Render callback type
 */
export type RenderCallback = () => void;
