/**
 * VRM动画系统 v2 - 动作队列 + 平滑过渡 + 随机待机
 * 参考duix-mobile的动画连贯性设计
 */

const VRMAnimation = {
    // 基础手臂角度（自然下垂51度）
    BASE_ARM_Z: 0.9,

    animations: {
        idle: {
            duration: 4000, loop: true,
            keyframes: [
                { time: 0, bones: { head: { x: 0, y: 0, z: 0 }, spine: { x: 0 } } },
                { time: 2000, bones: { head: { x: 0.02, y: 0.01, z: 0 }, spine: { x: 0.01 } } },
                { time: 4000, bones: { head: { x: 0, y: 0, z: 0 }, spine: { x: 0 } } }
            ]
        },
        nod: {
            duration: 800, loop: false,
            keyframes: [
                { time: 0, bones: { head: { x: 0 } } },
                { time: 200, bones: { head: { x: -0.15 } } },
                { time: 400, bones: { head: { x: 0.05 } } },
                { time: 600, bones: { head: { x: -0.1 } } },
                { time: 800, bones: { head: { x: 0 } } }
            ]
        },
        headShake: {
            duration: 1000, loop: false,
            keyframes: [
                { time: 0, bones: { head: { y: 0 } } },
                { time: 200, bones: { head: { y: -0.15 } } },
                { time: 400, bones: { head: { y: 0.15 } } },
                { time: 600, bones: { head: { y: -0.1 } } },
                { time: 800, bones: { head: { y: 0.05 } } },
                { time: 1000, bones: { head: { y: 0 } } }
            ]
        },
        bow: {
            duration: 1500, loop: false,
            keyframes: [
                { time: 0, bones: { spine: { x: 0 }, head: { x: 0 } } },
                { time: 500, bones: { spine: { x: -0.15 }, head: { x: 0.1 } } },
                { time: 1000, bones: { spine: { x: -0.15 }, head: { x: 0.1 } } },
                { time: 1500, bones: { spine: { x: 0 }, head: { x: 0 } } }
            ]
        },
        wave: {
            duration: 1200, loop: false,
            keyframes: [
                { time: 0, bones: { rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } },
                { time: 200, bones: { rightUpperArm: { z: -2.0, x: -0.2 }, rightLowerArm: { x: -0.3 }, rightHand: { z: 0.3 } } },
                { time: 400, bones: { rightUpperArm: { z: -1.8, x: -0.2 }, rightLowerArm: { x: -0.3 }, rightHand: { z: -0.3 } } },
                { time: 600, bones: { rightUpperArm: { z: -2.0, x: -0.2 }, rightLowerArm: { x: -0.3 }, rightHand: { z: 0.3 } } },
                { time: 800, bones: { rightUpperArm: { z: -1.8, x: -0.2 }, rightLowerArm: { x: -0.3 }, rightHand: { z: -0.3 } } },
                { time: 1000, bones: { rightUpperArm: { z: -1.5, x: -0.1 }, rightLowerArm: { x: -0.2 }, rightHand: { z: 0 } } },
                { time: 1200, bones: { rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } }
            ]
        },
        pointRight: {
            duration: 1000, loop: false,
            keyframes: [
                { time: 0, bones: { rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } },
                { time: 400, bones: { rightUpperArm: { z: -1.6, x: -0.1 }, rightLowerArm: { x: -0.5 }, rightHand: { z: 0.1 } } },
                { time: 700, bones: { rightUpperArm: { z: -1.6, x: -0.1 }, rightLowerArm: { x: -0.5 }, rightHand: { z: 0.1 } } },
                { time: 1000, bones: { rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } }
            ]
        },
        pointLeft: {
            duration: 1000, loop: false,
            keyframes: [
                { time: 0, bones: { leftUpperArm: { z: 0.9 }, leftLowerArm: { z: 0 }, leftHand: { z: 0 } } },
                { time: 400, bones: { leftUpperArm: { z: 1.6, x: -0.1 }, leftLowerArm: { x: -0.5 }, leftHand: { z: -0.1 } } },
                { time: 700, bones: { leftUpperArm: { z: 1.6, x: -0.1 }, leftLowerArm: { x: -0.5 }, leftHand: { z: -0.1 } } },
                { time: 1000, bones: { leftUpperArm: { z: 0.9 }, leftLowerArm: { z: 0 }, leftHand: { z: 0 } } }
            ]
        },
        openArms: {
            duration: 1500, loop: false,
            keyframes: [
                { time: 0, bones: { leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 }, leftLowerArm: { z: 0 }, rightLowerArm: { z: 0 } } },
                { time: 500, bones: { leftUpperArm: { z: 2.0, x: -0.1 }, rightUpperArm: { z: -2.0, x: -0.1 }, leftLowerArm: { x: -0.2 }, rightLowerArm: { x: -0.2 } } },
                { time: 1000, bones: { leftUpperArm: { z: 2.0, x: -0.1 }, rightUpperArm: { z: -2.0, x: -0.1 }, leftLowerArm: { x: -0.2 }, rightLowerArm: { x: -0.2 } } },
                { time: 1500, bones: { leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 }, leftLowerArm: { z: 0 }, rightLowerArm: { z: 0 } } }
            ]
        },
        think: {
            duration: 2000, loop: false,
            keyframes: [
                { time: 0, bones: { head: { z: 0 }, rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 } } },
                { time: 500, bones: { head: { z: 0.08 }, rightUpperArm: { z: -1.5, x: -0.1 }, rightLowerArm: { x: -0.8 } } },
                { time: 1500, bones: { head: { z: 0.08 }, rightUpperArm: { z: -1.5, x: -0.1 }, rightLowerArm: { x: -0.8 } } },
                { time: 2000, bones: { head: { z: 0 }, rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 } } }
            ]
        },
        explain: {
            duration: 1500, loop: false,
            keyframes: [
                { time: 0, bones: { rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } },
                { time: 300, bones: { rightUpperArm: { z: -1.4, x: -0.1 }, rightLowerArm: { x: -0.4 }, rightHand: { z: 0.2 } } },
                { time: 600, bones: { rightUpperArm: { z: -1.2, x: -0.1 }, rightLowerArm: { x: -0.3 }, rightHand: { z: -0.15 } } },
                { time: 900, bones: { rightUpperArm: { z: -1.4, x: -0.1 }, rightLowerArm: { x: -0.4 }, rightHand: { z: 0.2 } } },
                { time: 1200, bones: { rightUpperArm: { z: -1.2, x: -0.1 }, rightLowerArm: { x: -0.3 }, rightHand: { z: 0 } } },
                { time: 1500, bones: { rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } }
            ]
        },
        happy: {
            duration: 1200, loop: false,
            keyframes: [
                { time: 0, bones: { head: { x: 0, z: 0 }, spine: { x: 0 }, leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 } } },
                { time: 300, bones: { head: { x: -0.06, z: 0.03 }, spine: { x: -0.02 }, leftUpperArm: { z: 1.2 }, rightUpperArm: { z: -1.2 } } },
                { time: 600, bones: { head: { x: 0.03, z: -0.03 }, spine: { x: 0.02 }, leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 } } },
                { time: 900, bones: { head: { x: -0.03, z: 0.02 }, spine: { x: -0.02 }, leftUpperArm: { z: 1.2 }, rightUpperArm: { z: -1.2 } } },
                { time: 1200, bones: { head: { x: 0, z: 0 }, spine: { x: 0 }, leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 } } }
            ]
        },
        sad: {
            duration: 2000, loop: false,
            keyframes: [
                { time: 0, bones: { head: { x: 0 }, spine: { x: 0 }, leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 } } },
                { time: 600, bones: { head: { x: 0.1 }, spine: { x: 0.06 }, leftUpperArm: { z: 0.7 }, rightUpperArm: { z: -0.7 } } },
                { time: 1400, bones: { head: { x: 0.1 }, spine: { x: 0.06 }, leftUpperArm: { z: 0.7 }, rightUpperArm: { z: -0.7 } } },
                { time: 2000, bones: { head: { x: 0 }, spine: { x: 0 }, leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 } } }
            ]
        },
        surprised: {
            duration: 1000, loop: false,
            keyframes: [
                { time: 0, bones: { head: { x: 0 }, leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 }, leftLowerArm: { z: 0 }, rightLowerArm: { z: 0 } } },
                { time: 200, bones: { head: { x: -0.06 }, leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 }, leftLowerArm: { x: -0.3 }, rightLowerArm: { x: -0.3 } } },
                { time: 600, bones: { head: { x: -0.06 }, leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 }, leftLowerArm: { x: -0.3 }, rightLowerArm: { x: -0.3 } } },
                { time: 1000, bones: { head: { x: 0 }, leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 }, leftLowerArm: { z: 0 }, rightLowerArm: { z: 0 } } }
            ]
        },
        talking: {
            duration: 2000, loop: true,
            keyframes: [
                { time: 0, bones: { head: { x: 0, y: 0, z: 0 }, rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 } } },
                { time: 500, bones: { head: { x: 0.015, y: 0.015, z: 0 }, rightUpperArm: { z: -0.95 }, rightLowerArm: { x: -0.1 } } },
                { time: 1000, bones: { head: { x: -0.015, y: -0.015, z: 0.01 }, rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 } } },
                { time: 1500, bones: { head: { x: 0.01, y: 0.01, z: -0.01 }, rightUpperArm: { z: -0.95 }, rightLowerArm: { x: -0.1 } } },
                { time: 2000, bones: { head: { x: 0, y: 0, z: 0 }, rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 } } }
            ]
        },
        waveHand: {
            duration: 1500, loop: false,
            keyframes: [
                { time: 0, bones: { rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } },
                { time: 300, bones: { rightUpperArm: { z: -2.2, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: 0 } } },
                { time: 500, bones: { rightUpperArm: { z: -2.2, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: 0.4 } } },
                { time: 700, bones: { rightUpperArm: { z: -2.2, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: -0.4 } } },
                { time: 900, bones: { rightUpperArm: { z: -2.2, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: 0.4 } } },
                { time: 1100, bones: { rightUpperArm: { z: -2.2, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: -0.4 } } },
                { time: 1300, bones: { rightUpperArm: { z: -1.5, x: -0.1 }, rightLowerArm: { x: -0.1 }, rightHand: { z: 0 } } },
                { time: 1500, bones: { rightUpperArm: { z: -0.9 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } }
            ]
        },
        clap: {
            duration: 1200, loop: false,
            keyframes: [
                { time: 0, bones: { leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 }, leftLowerArm: { x: -0.3 }, rightLowerArm: { x: -0.3 } } },
                { time: 200, bones: { leftUpperArm: { z: 1.2, x: -0.5 }, rightUpperArm: { z: -1.2, x: -0.5 }, leftLowerArm: { x: -1.2 }, rightLowerArm: { x: -1.2 } } },
                { time: 400, bones: { leftUpperArm: { z: 1.1, x: -0.5 }, rightUpperArm: { z: -1.1, x: -0.5 }, leftLowerArm: { x: -1.0 }, rightLowerArm: { x: -1.0 } } },
                { time: 600, bones: { leftUpperArm: { z: 1.2, x: -0.5 }, rightUpperArm: { z: -1.2, x: -0.5 }, leftLowerArm: { x: -1.2 }, rightLowerArm: { x: -1.2 } } },
                { time: 800, bones: { leftUpperArm: { z: 1.1, x: -0.5 }, rightUpperArm: { z: -1.1, x: -0.5 }, leftLowerArm: { x: -1.0 }, rightLowerArm: { x: -1.0 } } },
                { time: 1000, bones: { leftUpperArm: { z: 1.0, x: -0.3 }, rightUpperArm: { z: -1.0, x: -0.3 }, leftLowerArm: { x: -0.6 }, rightLowerArm: { x: -0.6 } } },
                { time: 1200, bones: { leftUpperArm: { z: 0.9 }, rightUpperArm: { z: -0.9 }, leftLowerArm: { x: -0.3 }, rightLowerArm: { x: -0.3 } } }
            ]
        }
    },

    easing: {
        linear: t => t,
        easeInQuad: t => t * t,
        easeOutQuad: t => t * (2 - t),
        easeInOutCubic: t => t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1,
        easeOutElastic: t => Math.pow(2, -10 * t) * Math.sin((t - 0.075) * 2 * Math.PI / 0.3) + 1
    },

    // 动作队列（参考duix-mobile）
    _actionQueue: [],
    _isPlayingAction: false,
    _blendDuration: 150,  // 过渡混合时间ms

    currentAnimation: null,
    animationStartTime: 0,
    isPlaying: false,
    onComplete: null,

    /**
     * 播放动画（支持队列）
     */
    play(animationName, vrmModel, onComplete = null, easingType = 'easeInOutCubic') {
        const animation = this.animations[animationName];
        if (!animation) {
            console.warn(`[VRMAnimation] Not found: ${animationName}`);
            if (onComplete) onComplete();
            return;
        }
        this.currentAnimation = animation;
        this.animationStartTime = Date.now();
        this.isPlaying = true;
        this.onComplete = onComplete;
        this.currentEasing = this.easing[easingType] || this.easing.easeInOutCubic;
    },

    /**
     * 加入动作队列，顺序播放
     */
    queueAction(actionName) {
        this._actionQueue.push(actionName);
        if (!this._isPlayingAction) this._playNextInQueue();
    },

    _playNextInQueue() {
        if (this._actionQueue.length === 0) {
            this._isPlayingAction = false;
            return;
        }
        this._isPlayingAction = true;
        const action = this._actionQueue.shift();
        // 通过全局VRMBridge触发
        if (window.VRMBridge && window.VRMBridge.vrmModel) {
            this.play(action, window.VRMBridge.vrmModel, () => {
                setTimeout(() => this._playNextInQueue(), this._blendDuration);
            });
        }
    },

    stop() {
        this.isPlaying = false;
        this.currentAnimation = null;
        this._actionQueue = [];
        this._isPlayingAction = false;
    },

    /**
     * 更新动画（每帧调用）
     */
    update(vrmModel) {
        if (!this.isPlaying || !this.currentAnimation || !vrmModel || !vrmModel.humanoid) return;

        const now = Date.now();
        const elapsed = now - this.animationStartTime;
        const duration = this.currentAnimation.duration;
        let progress = elapsed / duration;

        if (this.currentAnimation.loop) {
            progress = progress % 1;
        } else if (progress >= 1) {
            progress = 1;
            this.isPlaying = false;
        }

        const eased = this.currentEasing(progress);
        const keyframes = this.currentAnimation.keyframes;
        let prev = keyframes[0], next = keyframes[keyframes.length - 1];

        for (let i = 0; i < keyframes.length - 1; i++) {
            if (eased >= keyframes[i].time / duration && eased <= keyframes[i + 1].time / duration) {
                prev = keyframes[i];
                next = keyframes[i + 1];
                break;
            }
        }

        const prevT = prev.time / duration;
        const nextT = next.time / duration;
        const t = Math.max(0, Math.min(1, (eased - prevT) / (nextT - prevT || 0.001)));

        const h = vrmModel.humanoid;
        for (const boneName in prev.bones) {
            const bone = h.getNormalizedBoneNode(boneName);
            if (!bone) continue;
            const pVal = prev.bones[boneName];
            const nVal = next.bones[boneName] || pVal;
            for (const axis in pVal) {
                bone.rotation[axis] = pVal[axis] + ((nVal[axis] !== undefined ? nVal[axis] : pVal[axis]) - pVal[axis]) * t;
            }
        }

        if (!this.isPlaying && this.onComplete) {
            const cb = this.onComplete;
            this.onComplete = null;
            cb();
        }
    },

    getAnimationNames() {
        return Object.keys(this.animations);
    }
};

window.VRMAnimation = VRMAnimation;
