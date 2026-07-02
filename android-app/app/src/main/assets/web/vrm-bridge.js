/**
 * VRM Bridge - Kotlin ↔ WebView JS 通信脚本
 */

const VRMBridge = {
    scene: null,
    camera: null,
    renderer: null,
    vrmModel: null,
    clock: new THREE.Clock(),
    currentExpression: 'neutral',
    lipSyncValue: 0,
    blinkTimer: 0,
    isBlinking: false,
    // 模型缓存：{ url: { vrm, scene } }
    _modelCache: {},
    _currentUrl: '',

    init() {
        console.log('[VRM] Initializing...');
        const canvas = document.getElementById('vrm-canvas');

        // 场景
        this.scene = new THREE.Scene();

        // 相机 - 更近的视角，让模型更大
        this.camera = new THREE.PerspectiveCamera(25, window.innerWidth / window.innerHeight, 0.1, 1000);
        this.camera.position.set(0, 1.3, 2.0);
        this.camera.lookAt(0, 1.0, 0);

        // 渲染器（alpha: true 启用透明背景，参考duix-mobile的透明渲染）
        this.renderer = new THREE.WebGLRenderer({
            canvas,
            alpha: true,
            antialias: true,
            premultipliedAlpha: false
        });
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        this.renderer.outputColorSpace = THREE.SRGBColorSpace;

        // 灯光 - 更亮
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
        this.scene.add(ambientLight);
        const mainLight = new THREE.DirectionalLight(0xffffff, 1.0);
        mainLight.position.set(2, 3, 4);
        this.scene.add(mainLight);
        const fillLight = new THREE.DirectionalLight(0xffffff, 0.4);
        fillLight.position.set(-2, 2, -3);
        this.scene.add(fillLight);
        const rimLight = new THREE.DirectionalLight(0x8888ff, 0.3);
        rimLight.position.set(0, 2, -4);
        this.scene.add(rimLight);

        window.addEventListener('resize', () => this.onResize());
        this.animate();

        document.getElementById('loading').style.display = 'none';
        console.log('[VRM] Init complete');

        if (window.AndroidBridge && window.AndroidBridge.onLibsReady) {
            window.AndroidBridge.onLibsReady();
        }
    },

    async loadModel(url) {
        try {
            // 如果同一个模型已显示，跳过
            if (url === this._currentUrl && this.vrmModel) {
                console.log('[VRM] Same model already displayed, skip');
                if (window.AndroidBridge && window.AndroidBridge.onVrmLoaded) {
                    window.AndroidBridge.onVrmLoaded(url);
                }
                return;
            }

            // 移除当前模型
            if (this.vrmModel) {
                this.scene.remove(this.vrmModel.scene);
                this.vrmModel = null;
                this._currentUrl = '';
            }

            // 检查缓存
            if (this._modelCache[url]) {
                console.log('[VRM] Cache hit, restoring model');
                const cached = this._modelCache[url];
                this.vrmModel = cached.vrm;
                this._currentUrl = url;
                this.scene.add(cached.vrm.scene);
                this._adjustCamera(cached.vrm);
                document.getElementById('loading').style.display = 'none';
                if (window.AndroidBridge && window.AndroidBridge.onVrmLoaded) {
                    window.AndroidBridge.onVrmLoaded(url);
                }
                return;
            }

            // 缓存未命中，加载模型
            console.log('[VRM] Loading model from:', url.substring(0, 80) + '...');
            document.getElementById('loading').style.display = 'block';
            document.getElementById('loading').querySelector('span').textContent = '加载模型中...';

            const loader = new THREE.GLTFLoader();
            loader.register((parser) => new THREE.VRMLoaderPlugin(parser));

            const gltf = await new Promise((resolve, reject) => {
                const timeout = setTimeout(() => {
                    reject(new Error('Model load timeout (15s)'));
                }, 15000);

                loader.load(
                    url,
                    (gltf) => {
                        clearTimeout(timeout);
                        console.log('[VRM] GLTF loaded successfully');
                        resolve(gltf);
                    },
                    (progress) => {
                        if (progress.total > 0) {
                            const percent = Math.round((progress.loaded / progress.total) * 100);
                            if (percent % 20 === 0) {
                                console.log('[VRM] Loading progress: ' + percent + '%');
                            }
                        }
                    },
                    (error) => {
                        clearTimeout(timeout);
                        console.error('[VRM] GLTF load error:', error);
                        reject(error);
                    }
                );
            });

            const vrm = gltf.userData.vrm;
            if (!vrm) {
                throw new Error('Failed to parse VRM model - no VRM data found');
            }

            console.log('[VRM] VRM parsed, humanoid:', vrm.humanoid ? 'ok' : 'MISSING');

            this.vrmModel = vrm;
            this._currentUrl = url;

            // 调整模型
            const box = new THREE.Box3().setFromObject(vrm.scene);
            const size = box.getSize(new THREE.Vector3());
            const center = box.getCenter(new THREE.Vector3());
            console.log('[VRM] Model size:', size.x.toFixed(2), size.y.toFixed(2), size.z.toFixed(2));

            const targetHeight = 0.6;
            const scale = targetHeight / size.y;
            vrm.scene.scale.setScalar(scale);
            vrm.scene.position.set(-center.x * scale, -box.min.y * scale - 0.1, 0);
            vrm.scene.rotation.y = Math.PI;

            this._adjustArmPose(vrm);
            this.scene.add(vrm.scene);
            this._adjustCamera(vrm);

            // 存入缓存
            this._modelCache[url] = { vrm: vrm };
            console.log('[VRM] Model cached, total cached:', Object.keys(this._modelCache).length);

            document.getElementById('loading').style.display = 'none';
            console.log('[VRM] Model added, scale=' + scale.toFixed(2));

            if (window.AndroidBridge && window.AndroidBridge.onVrmLoaded) {
                window.AndroidBridge.onVrmLoaded(url);
            }
        } catch (e) {
            console.error('[VRM] Model load error:', e.message);
            document.getElementById('loading').style.display = 'none';
            document.getElementById('vrm-canvas').style.display = 'none';
            document.getElementById('avatar-2d').style.display = 'block';

            if (window.AndroidBridge && window.AndroidBridge.onError) {
                window.AndroidBridge.onError(e.message);
            }
        }
    },

    // 调整相机对准模型脸部
    _adjustCamera(vrm) {
        const box = new THREE.Box3().setFromObject(vrm.scene);
        const scale = vrm.scene.scale.x;
        const headY = (box.max.y * scale) * 0.75;
        this.camera.position.set(0, headY + 0.2, 1.5);
        this.camera.lookAt(0, headY, 0);
    },

    setExpression(name) {
        if (!this.vrmModel) return;
        const manager = this.vrmModel.expressionManager;
        if (!manager) {
            console.log('[VRM] No expressionManager');
            return;
        }
        console.log('[VRM] Setting expression:', name);
        this.currentExpression = name;

        // 重置所有表情
        const allExprs = ['happy', 'angry', 'sad', 'relaxed', 'surprised', 'neutral',
                          'aa', 'ih', 'ou', 'ee', 'oh', 'blink', 'joy', 'sorrow'];
        allExprs.forEach(expr => {
            try { manager.setValue(expr, 0); } catch(e) {}
        });

        // 设置目标表情
        const exprMap = {
            'happy': 'happy', 'joy': 'happy',
            'sad': 'sad', 'sorrow': 'sad',
            'angry': 'angry',
            'surprised': 'surprised', 'surprise': 'surprised',
            'relaxed': 'relaxed',
            'greeting': 'happy',
            'neutral': 'neutral'
        };
        const targetExpr = exprMap[name] || 'neutral';
        try {
            // 平滑过渡到目标表情
            this._animateExpression(manager, targetExpr, 0, 1.0, 300);
        } catch(e) {
            console.log('[VRM] Expression not found:', targetExpr);
        }
    },

    /**
     * 平滑过渡表情
     */
    _animateExpression(manager, exprName, from, to, duration) {
        const startTime = Date.now();
        const animate = () => {
            const progress = Math.min((Date.now() - startTime) / duration, 1);
            const eased = progress < 0.5
                ? 2 * progress * progress
                : 1 - Math.pow(-2 * progress + 2, 2) / 2;
            const value = from + (to - from) * eased;
            try {
                manager.setValue(exprName, value);
            } catch(e) {}
            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };
        animate();
    },

    playAction(name) {
        if (!this.vrmModel) return;
        console.log('[VRM] Playing action:', name);
        const humanoid = this.vrmModel.humanoid;
        if (!humanoid) return;

        this._isPlayingAction = true;

        switch(name) {
            case 'nod':
                // 点头 - 头部上下运动
                this._animateBone(humanoid, 'head', 'x', 0, -0.2, 400, () => {
                    this._isPlayingAction = false;
                });
                break;
            case 'wave':
                // 摇手 - 右臂摆动
                this._animateBone(humanoid, 'rightUpperArm', 'z', -0.6, -1.8, 500, () => {
                    this._isPlayingAction = false;
                });
                this._animateBone(humanoid, 'rightHand', 'z', 0, 0.5, 300);
                break;
            case 'bow':
                // 鞠躬 - 身体前倾
                this._animateBone(humanoid, 'spine', 'x', 0, 0.2, 600, () => {
                    this._isPlayingAction = false;
                });
                break;
            default:
                console.log('[VRM] Unknown action:', name);
                this._isPlayingAction = false;
        }
    },

    // 标记是否需要持续应用姿势
    _poseApplied: false,

    /**
     * 调整VRM模型手臂 - 从T-pose转为自然站立姿势
     * VRM T-pose: 手臂水平伸展
     * 自然站立: 手臂下垂约80度
     */
    _adjustArmPose(vrm) {
        try {
            const h = vrm.humanoid;
            if (!h) {
                console.log('[VRM] No humanoid found');
                return;
            }

            // 上臂：从T-pose旋转到自然下垂
            const leftUpperArm = h.getNormalizedBoneNode('leftUpperArm');
            const rightUpperArm = h.getNormalizedBoneNode('rightUpperArm');

            if (leftUpperArm) {
                leftUpperArm.rotation.set(0, 0, 1.4);
                console.log('[VRM] Left upper arm z:', leftUpperArm.rotation.z);
            }
            if (rightUpperArm) {
                rightUpperArm.rotation.set(0, 0, -1.4);
                console.log('[VRM] Right upper arm z:', rightUpperArm.rotation.z);
            }

            // 前臂自然放松
            const leftLowerArm = h.getNormalizedBoneNode('leftLowerArm');
            const rightLowerArm = h.getNormalizedBoneNode('rightLowerArm');
            if (leftLowerArm) leftLowerArm.rotation.set(0, 0, 0);
            if (rightLowerArm) rightLowerArm.rotation.set(0, 0, 0);

            // 手腕自然放松
            const leftHand = h.getNormalizedBoneNode('leftHand');
            const rightHand = h.getNormalizedBoneNode('rightHand');
            if (leftHand) leftHand.rotation.set(0, 0, 0);
            if (rightHand) rightHand.rotation.set(0, 0, 0);

            this._poseApplied = true;
            console.log('[VRM] Arms set to natural pose - DONE');
        } catch (e) {
            console.error('[VRM] Arm error:', e.message, e.stack);
        }
    },

    _animateRotation(axis, min, max, duration) {
        const scene = this.vrmModel.scene;
        const startRot = scene.rotation[axis];
        const startTime = Date.now();
        const animate = () => {
            const elapsed = Date.now() - startTime;
            const progress = elapsed / duration;
            if (progress < 0.5) {
                scene.rotation[axis] = startRot + min * Math.sin(progress * Math.PI * 2);
            } else if (progress < 1) {
                scene.rotation[axis] = startRot;
            } else {
                scene.rotation[axis] = startRot;
                return;
            }
            requestAnimationFrame(animate);
        };
        animate();
    },

    /**
     * 口型同步 - 根据音频音量驱动嘴巴
     * value: 0~1，0闭嘴，1张最大
     */
    setLipSync(value) {
        this.lipSyncValue = Math.max(0, Math.min(1, value));
        if (!this.vrmModel || !this.vrmModel.expressionManager) return;
        const manager = this.vrmModel.expressionManager;

        try {
            // 重置所有口型
            ['aa', 'ih', 'ou', 'ee', 'oh'].forEach(m => {
                try { manager.setValue(m, 0); } catch(e) {}
            });

            // 根据音量模拟不同口型
            const v = this.lipSyncValue;
            if (v < 0.1) {
                // 闭嘴
            } else if (v < 0.3) {
                // 小口 - "ih" 音
                manager.setValue('ih', v * 2);
            } else if (v < 0.6) {
                // 中口 - "aa" 音（张大嘴）
                manager.setValue('aa', v * 1.2);
            } else {
                // 大口 - "ou" 音
                manager.setValue('ou', v * 0.8);
                manager.setValue('aa', v * 0.4);
            }
        } catch(e) {}
    },

    /**
     * 模拟说话口型（优化版 - 更自然的口型变化）
     * duration: 说话时长(毫秒)
     */
    startTalking(duration) {
        if (this._talkingTimer) clearInterval(this._talkingTimer);
        const startTime = Date.now();
        let lastMouthValue = 0;

        this._talkingTimer = setInterval(() => {
            const elapsed = Date.now() - startTime;
            if (elapsed > duration) {
                this.stopTalking();
                return;
            }

            // 更自然的口型模拟：多种节奏混合
            const cycle1 = elapsed % 180;  // 快节奏
            const cycle2 = elapsed % 400;  // 中节奏
            const cycle3 = elapsed % 800;  // 慢节奏

            // 混合不同频率，产生更自然的口型
            let value = 0;
            if (cycle1 < 70) {
                value = Math.sin(cycle1 / 70 * Math.PI) * 0.6;
            } else if (cycle2 < 150) {
                value = Math.sin(cycle2 / 150 * Math.PI) * 0.4;
            } else if (cycle3 < 300) {
                value = Math.sin(cycle3 / 300 * Math.PI) * 0.3;
            }

            // 平滑过渡，避免突变
            lastMouthValue = lastMouthValue * 0.7 + value * 0.3;
            this.setLipSync(lastMouthValue);

            // 头部轻微动作（说话时自然摆动）
            if (this.vrmModel && this.vrmModel.humanoid) {
                const head = this.vrmModel.humanoid.getNormalizedBoneNode('head');
                if (head) {
                    const headCycle = elapsed % 2500;
                    const headValue = Math.sin(headCycle / 2500 * Math.PI * 2) * 0.03;
                    head.rotation.x = headValue;
                    const headY = Math.sin(headCycle / 3500 * Math.PI * 2) * 0.02;
                    head.rotation.y = headY;
                }
            }
        }, 30);
    },

    stopTalking() {
        if (this._talkingTimer) {
            clearInterval(this._talkingTimer);
            this._talkingTimer = null;
        }
        this.setLipSync(0);

        // 重置头部位置
        if (this.vrmModel && this.vrmModel.humanoid) {
            const head = this.vrmModel.humanoid.getNormalizedBoneNode('head');
            if (head) {
                head.rotation.x = 0;
            }
        }
    },

    /**
     * 导游动作 - 讲解时的手势（参考duix-mobile的动作系统）
     */
    // 可用动作列表
    _availableActions: ['point_right', 'point_left', 'open_arms', 'nod', 'bow', 'wave', 'think', 'explain'],

    /**
     * 导游动作 - 使用动画系统
     */
    playGuideAction(action) {
        if (!this.vrmModel) return;

        // 映射动作名称到动画名称
        const actionMap = {
            'point_right': 'pointRight',
            'point_left': 'pointLeft',
            'open_arms': 'openArms',
            'nod': 'nod',
            'bow': 'bow',
            'wave': 'waveHand',
            'think': 'think',
            'explain': 'explain',
            'idle': 'idle'
        };

        const animName = actionMap[action] || action;
        console.log('[VRM] Playing action:', action, '-> animation:', animName);

        if (window.VRMAnimation && VRMAnimation.animations[animName]) {
            VRMAnimation.play(animName, this.vrmModel, () => {
                console.log('[VRM] Action completed:', action);
                if (window.AndroidBridge && window.AndroidBridge.onMotionComplete) {
                    window.AndroidBridge.onMotionComplete(action);
                }
            });
        } else {
            console.warn('[VRM] Animation not found:', animName);
        }
    },

    /**
     * 随机播放一个动作
     */
    playRandomAction() {
        if (this._isPlayingAction) return;
        const actions = this._availableActions;
        const randomIndex = Math.floor(Math.random() * actions.length);
        const action = actions[randomIndex];
        console.log('[VRM] Playing random action:', action);
        this.playGuideAction(action);
    },

    /**
     * 获取可用动作列表
     */
    getAvailableActions() {
        return this._availableActions;
    },

    /**
     * 播放指定动画
     * @param {string} animName - 动画名称
     */
    playAnimation(animName) {
        if (!this.vrmModel) return;
        if (window.VRMAnimation && VRMAnimation.animations[animName]) {
            VRMAnimation.play(animName, this.vrmModel, () => {
                console.log('[VRM] Animation completed:', animName);
            });
        }
    },

    /**
     * 停止当前动画
     */
    stopAnimation() {
        if (window.VRMAnimation) {
            VRMAnimation.stop();
        }
    },

    /**
     * 获取所有可用动画名称
     */
    getAnimationNames() {
        if (window.VRMAnimation) {
            return VRMAnimation.getAnimationNames();
        }
        return [];
    },

    _resetPose(humanoid) {
        // 重置头部和躯干
        ['head', 'spine', 'chest'].forEach(name => {
            const bone = humanoid.getNormalizedBoneNode(name);
            if (bone) bone.rotation.set(0, 0, 0);
        });

        // 上臂自然下垂
        const leftUpperArm = humanoid.getNormalizedBoneNode('leftUpperArm');
        const rightUpperArm = humanoid.getNormalizedBoneNode('rightUpperArm');
        if (leftUpperArm) leftUpperArm.rotation.set(0, 0, 1.4);
        if (rightUpperArm) rightUpperArm.rotation.set(0, 0, -1.4);

        // 前臂
        const leftLowerArm = humanoid.getNormalizedBoneNode('leftLowerArm');
        const rightLowerArm = humanoid.getNormalizedBoneNode('rightLowerArm');
        if (leftLowerArm) leftLowerArm.rotation.set(0, 0, 0);
        if (rightLowerArm) rightLowerArm.rotation.set(0, 0, 0);

        // 手腕
        const leftHand = humanoid.getNormalizedBoneNode('leftHand');
        const rightHand = humanoid.getNormalizedBoneNode('rightHand');
        if (leftHand) leftHand.rotation.set(0, 0, 0);
        if (rightHand) rightHand.rotation.set(0, 0, 0);
    },

    triggerBlink() {
        if (this.isBlinking) return;
        this.isBlinking = true;
        const duration = 150;
        const startTime = Date.now();
        const blink = () => {
            const elapsed = Date.now() - startTime;
            const progress = elapsed / duration;
            if (progress < 0.5) {
                if (this.vrmModel && this.vrmModel.expressionManager) {
                    this.vrmModel.expressionManager.setValue('blink', progress * 2);
                }
            } else if (progress < 1.0) {
                if (this.vrmModel && this.vrmModel.expressionManager) {
                    this.vrmModel.expressionManager.setValue('blink', (1.0 - progress) * 2);
                }
            } else {
                if (this.vrmModel && this.vrmModel.expressionManager) {
                    this.vrmModel.expressionManager.setValue('blink', 0);
                }
                this.isBlinking = false;
                return;
            }
            requestAnimationFrame(blink);
        };
        blink();
    },

    // 标记是否有动画正在播放
    _isPlayingAction: false,
    _currentAnimationName: null,

    animate() {
        requestAnimationFrame(() => this.animate());
        const delta = this.clock.getDelta();
        const time = this.clock.getElapsedTime();

        if (this.vrmModel) {
            // VRM更新
            if (this.vrmModel.update) {
                this.vrmModel.update(delta);
            }

            // 更新动画系统
            if (window.VRMAnimation) {
                VRMAnimation.update(this.vrmModel);
                this._isPlayingAction = VRMAnimation.isPlaying;
            }

            // 持续应用手臂姿势（防止被VRM update重置）
            // 只在没有动作播放时应用
            if (this._poseApplied && !this._isPlayingAction) {
                this._maintainPose();
            }

            // 待机动画 - 轻微呼吸浮动
            if (this.vrmModel && this.vrmModel.scene) {
                this.vrmModel.scene.position.y += Math.sin(time * 1.5) * 0.0003;
            }

            // 待机动画 - 轻微左右摇摆（只在非说话状态）
            if (this.vrmModel && this.vrmModel.scene && !this._talkingTimer) {
                this.vrmModel.scene.rotation.y = Math.PI + Math.sin(time * 0.5) * 0.02;
            }

            // 眨眼
            this.blinkTimer += delta;
            if (this.blinkTimer > 3 + Math.random() * 2) {
                this.blinkTimer = 0;
                this.triggerBlink();
            }
        }

        this.renderer.render(this.scene, this.camera);
    },

    /**
     * 维持自然姿势
     */
    _maintainPose() {
        if (!this.vrmModel || !this.vrmModel.humanoid) return;
        const h = this.vrmModel.humanoid;

        const leftUpperArm = h.getNormalizedBoneNode('leftUpperArm');
        const rightUpperArm = h.getNormalizedBoneNode('rightUpperArm');
        if (leftUpperArm) leftUpperArm.rotation.set(0, 0, 1.4);
        if (rightUpperArm) rightUpperArm.rotation.set(0, 0, -1.4);

        const leftLowerArm = h.getNormalizedBoneNode('leftLowerArm');
        const rightLowerArm = h.getNormalizedBoneNode('rightLowerArm');
        if (leftLowerArm) leftLowerArm.rotation.set(0, 0, 0);
        if (rightLowerArm) rightLowerArm.rotation.set(0, 0, 0);

        const leftHand = h.getNormalizedBoneNode('leftHand');
        const rightHand = h.getNormalizedBoneNode('rightHand');
        if (leftHand) leftHand.rotation.set(0, 0, 0);
        if (rightHand) rightHand.rotation.set(0, 0, 0);
    },

    onResize() {
        this.camera.aspect = window.innerWidth / window.innerHeight;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(window.innerWidth, window.innerHeight);
    },

    playArmAnimation(type) {},
    playHeadAnimation(type) {},
    playBodyAnimation(type) {}
};

// Global functions for Kotlin bridge
function loadModel(url) { VRMBridge.loadModel(url); }
function setExpression(expr) { VRMBridge.setExpression(expr); }
function playAction(action) { VRMBridge.playAction(action); }
function playGuideAction(action) { VRMBridge.playGuideAction(action); }
function setLipSync(value) { VRMBridge.setLipSync(value); }
function startTalking(duration) { VRMBridge.startTalking(duration); }
function stopTalking() { VRMBridge.stopTalking(); }
function triggerBlink() { VRMBridge.triggerBlink(); }
function takeSnapshot() { VRMBridge.takeSnapshot(); }

// New animation functions
function playAnimation(animName) { VRMBridge.playAnimation(animName); }
function stopAnimation() { VRMBridge.stopAnimation(); }
function getAnimationNames() { return VRMBridge.getAnimationNames(); }

// Configuration update function
function updateConfig(armAngle, idleAnimation, talkAnimation, animationIntensity) {
    console.log('[VRM] Updating config:', { armAngle, idleAnimation, talkAnimation, animationIntensity });
    // Store config for later use
    VRMBridge._config = { armAngle, idleAnimation, talkAnimation, animationIntensity };
}

// Debug function - call from console to test pose
function debugPose() {
    if (!VRMBridge.vrmModel || !VRMBridge.vrmModel.humanoid) {
        console.log('[DEBUG] No model loaded');
        return;
    }
    const h = VRMBridge.vrmModel.humanoid;
    const bones = ['leftUpperArm', 'rightUpperArm', 'leftLowerArm', 'rightLowerArm',
                   'leftHand', 'rightHand', 'head', 'spine', 'chest'];
    bones.forEach(name => {
        const bone = h.getNormalizedBoneNode(name);
        if (bone) {
            console.log(`[DEBUG] ${name}: x=${bone.rotation.x.toFixed(2)}, y=${bone.rotation.y.toFixed(2)}, z=${bone.rotation.z.toFixed(2)}`);
        } else {
            console.log(`[DEBUG] ${name}: NOT FOUND`);
        }
    });
}

// Force apply pose - call from console to manually set pose
function forcePose() {
    VRMBridge._adjustArmPose(VRMBridge.vrmModel);
    console.log('[DEBUG] Pose forced');
}

// Random action (参考duix-mobile的startRandomMotion)
function playRandomAction() { VRMBridge.playRandomAction(); }

// Get available actions (参考duix-mobile的ModelInfo.motionRegions)
function getAvailableActions() {
    const actions = VRMBridge.getAvailableActions();
    console.log('[VRM] Available actions:', actions);
    return actions;
}

// Get available animations from animation library
function listAnimations() {
    const anims = VRMBridge.getAnimationNames();
    console.log('[VRM] Available animations:', anims);
    return anims;
}

// Initialize on load
window.addEventListener('DOMContentLoaded', () => VRMBridge.init());
