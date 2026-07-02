/**
 * VRM动画系统 - 基于关键帧的动画库
 * 适配VRM模型的骨骼结构
 */

const VRMAnimation = {
    // VRM骨骼名称映射
    BONES: {
        // 躯干
        hips: 'hips',
        spine: 'spine',
        chest: 'chest',
        upperChest: 'upperChest',
        neck: 'neck',
        head: 'head',

        // 左臂
        leftShoulder: 'leftShoulder',
        leftUpperArm: 'leftUpperArm',
        leftLowerArm: 'leftLowerArm',
        leftHand: 'leftHand',

        // 右臂
        rightShoulder: 'rightShoulder',
        rightUpperArm: 'rightUpperArm',
        rightLowerArm: 'rightLowerArm',
        rightHand: 'rightHand',

        // 左腿
        leftUpperLeg: 'leftUpperLeg',
        leftLowerLeg: 'leftLowerLeg',
        leftFoot: 'leftFoot',

        // 右腿
        rightUpperLeg: 'rightUpperLeg',
        rightLowerLeg: 'rightLowerLeg',
        rightFoot: 'rightFoot'
    },

    // 预定义动画库
    animations: {
        // 空闲待机 - 轻微呼吸和摇摆
        idle: {
            duration: 4000,
            loop: true,
            keyframes: [
                { time: 0, bones: { head: { x: 0, y: 0, z: 0 }, spine: { x: 0 } } },
                { time: 2000, bones: { head: { x: 0.03, y: 0.02, z: 0 }, spine: { x: 0.02 } } },
                { time: 4000, bones: { head: { x: 0, y: 0, z: 0 }, spine: { x: 0 } } }
            ]
        },

        // 点头
        nod: {
            duration: 800,
            loop: false,
            keyframes: [
                { time: 0, bones: { head: { x: 0 } } },
                { time: 200, bones: { head: { x: -0.15 } } },
                { time: 400, bones: { head: { x: 0.05 } } },
                { time: 600, bones: { head: { x: -0.1 } } },
                { time: 800, bones: { head: { x: 0 } } }
            ]
        },

        // 摇头
        headShake: {
            duration: 1000,
            loop: false,
            keyframes: [
                { time: 0, bones: { head: { y: 0 } } },
                { time: 200, bones: { head: { y: -0.15 } } },
                { time: 400, bones: { head: { y: 0.15 } } },
                { time: 600, bones: { head: { y: -0.1 } } },
                { time: 800, bones: { head: { y: 0.05 } } },
                { time: 1000, bones: { head: { y: 0 } } }
            ]
        },

        // 鞠躬 - 脊柱后仰（X轴负方向）
        bow: {
            duration: 1500,
            loop: false,
            keyframes: [
                { time: 0, bones: { spine: { x: 0 }, head: { x: 0 } } },
                { time: 500, bones: { spine: { x: -0.15 }, head: { x: 0.1 } } },
                { time: 1000, bones: { spine: { x: -0.15 }, head: { x: 0.1 } } },
                { time: 1500, bones: { spine: { x: 0 }, head: { x: 0 } } }
            ]
        },

        // 挥手（右手）- 基于自然站立姿势(1.4)
        wave: {
            duration: 1200,
            loop: false,
            keyframes: [
                { time: 0, bones: { rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } },
                { time: 200, bones: { rightUpperArm: { z: -2.5, x: -0.2 }, rightLowerArm: { x: -0.3 }, rightHand: { z: 0.3 } } },
                { time: 400, bones: { rightUpperArm: { z: -2.3, x: -0.2 }, rightLowerArm: { x: -0.3 }, rightHand: { z: -0.3 } } },
                { time: 600, bones: { rightUpperArm: { z: -2.5, x: -0.2 }, rightLowerArm: { x: -0.3 }, rightHand: { z: 0.3 } } },
                { time: 800, bones: { rightUpperArm: { z: -2.3, x: -0.2 }, rightLowerArm: { x: -0.3 }, rightHand: { z: -0.3 } } },
                { time: 1000, bones: { rightUpperArm: { z: -1.8, x: -0.1 }, rightLowerArm: { x: -0.2 }, rightHand: { z: 0 } } },
                { time: 1200, bones: { rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } }
            ]
        },

        // 指向右边
        pointRight: {
            duration: 1000,
            loop: false,
            keyframes: [
                { time: 0, bones: { rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } },
                { time: 400, bones: { rightUpperArm: { z: -2.0, x: -0.1 }, rightLowerArm: { x: -0.5 }, rightHand: { z: 0.1 } } },
                { time: 700, bones: { rightUpperArm: { z: -2.0, x: -0.1 }, rightLowerArm: { x: -0.5 }, rightHand: { z: 0.1 } } },
                { time: 1000, bones: { rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } }
            ]
        },

        // 指向左边
        pointLeft: {
            duration: 1000,
            loop: false,
            keyframes: [
                { time: 0, bones: { leftUpperArm: { z: 1.4 }, leftLowerArm: { z: 0 }, leftHand: { z: 0 } } },
                { time: 400, bones: { leftUpperArm: { z: 2.0, x: -0.1 }, leftLowerArm: { x: -0.5 }, leftHand: { z: -0.1 } } },
                { time: 700, bones: { leftUpperArm: { z: 2.0, x: -0.1 }, leftLowerArm: { x: -0.5 }, leftHand: { z: -0.1 } } },
                { time: 1000, bones: { leftUpperArm: { z: 1.4 }, leftLowerArm: { z: 0 }, leftHand: { z: 0 } } }
            ]
        },

        // 张开双臂（欢迎）
        openArms: {
            duration: 1500,
            loop: false,
            keyframes: [
                { time: 0, bones: { leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 }, leftLowerArm: { z: 0 }, rightLowerArm: { z: 0 } } },
                { time: 600, bones: { leftUpperArm: { z: 2.5, x: -0.1 }, rightUpperArm: { z: -2.5, x: -0.1 }, leftLowerArm: { x: -0.2 }, rightLowerArm: { x: -0.2 } } },
                { time: 1000, bones: { leftUpperArm: { z: 2.5, x: -0.1 }, rightUpperArm: { z: -2.5, x: -0.1 }, leftLowerArm: { x: -0.2 }, rightLowerArm: { x: -0.2 } } },
                { time: 1500, bones: { leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 }, leftLowerArm: { z: 0 }, rightLowerArm: { z: 0 } } }
            ]
        },

        // 思考
        think: {
            duration: 2000,
            loop: false,
            keyframes: [
                { time: 0, bones: { head: { z: 0 }, rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 } } },
                { time: 500, bones: { head: { z: 0.08 }, rightUpperArm: { z: -1.8, x: -0.1 }, rightLowerArm: { x: -0.8 } } },
                { time: 1500, bones: { head: { z: 0.08 }, rightUpperArm: { z: -1.8, x: -0.1 }, rightLowerArm: { x: -0.8 } } },
                { time: 2000, bones: { head: { z: 0 }, rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 } } }
            ]
        },

        // 解释说明
        explain: {
            duration: 1500,
            loop: false,
            keyframes: [
                { time: 0, bones: { rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } },
                { time: 300, bones: { rightUpperArm: { z: -1.8, x: -0.1 }, rightLowerArm: { x: -0.4 }, rightHand: { z: 0.2 } } },
                { time: 600, bones: { rightUpperArm: { z: -1.6, x: -0.1 }, rightLowerArm: { x: -0.3 }, rightHand: { z: -0.15 } } },
                { time: 900, bones: { rightUpperArm: { z: -1.8, x: -0.1 }, rightLowerArm: { x: -0.4 }, rightHand: { z: 0.2 } } },
                { time: 1200, bones: { rightUpperArm: { z: -1.6, x: -0.1 }, rightLowerArm: { x: -0.3 }, rightHand: { z: 0 } } },
                { time: 1500, bones: { rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } }
            ]
        },

        // 开心
        happy: {
            duration: 1200,
            loop: false,
            keyframes: [
                { time: 0, bones: { head: { x: 0, z: 0 }, spine: { x: 0 }, leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 } } },
                { time: 300, bones: { head: { x: -0.08, z: 0.04 }, spine: { x: -0.03 }, leftUpperArm: { z: 1.6 }, rightUpperArm: { z: -1.6 } } },
                { time: 600, bones: { head: { x: 0.04, z: -0.04 }, spine: { x: 0.02 }, leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 } } },
                { time: 900, bones: { head: { x: -0.04, z: 0.03 }, spine: { x: -0.02 }, leftUpperArm: { z: 1.6 }, rightUpperArm: { z: -1.6 } } },
                { time: 1200, bones: { head: { x: 0, z: 0 }, spine: { x: 0 }, leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 } } }
            ]
        },

        // 悲伤
        sad: {
            duration: 2000,
            loop: false,
            keyframes: [
                { time: 0, bones: { head: { x: 0 }, spine: { x: 0 }, leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 } } },
                { time: 600, bones: { head: { x: 0.12 }, spine: { x: 0.08 }, leftUpperArm: { z: 1.2 }, rightUpperArm: { z: -1.2 } } },
                { time: 1400, bones: { head: { x: 0.12 }, spine: { x: 0.08 }, leftUpperArm: { z: 1.2 }, rightUpperArm: { z: -1.2 } } },
                { time: 2000, bones: { head: { x: 0 }, spine: { x: 0 }, leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 } } }
            ]
        },

        // 惊讶
        surprised: {
            duration: 1000,
            loop: false,
            keyframes: [
                { time: 0, bones: { head: { x: 0 }, leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 }, leftLowerArm: { z: 0 }, rightLowerArm: { z: 0 } } },
                { time: 200, bones: { head: { x: -0.08 }, leftUpperArm: { z: 1.8 }, rightUpperArm: { z: -1.8 }, leftLowerArm: { x: -0.4 }, rightLowerArm: { x: -0.4 } } },
                { time: 600, bones: { head: { x: -0.08 }, leftUpperArm: { z: 1.8 }, rightUpperArm: { z: -1.8 }, leftLowerArm: { x: -0.4 }, rightLowerArm: { x: -0.4 } } },
                { time: 1000, bones: { head: { x: 0 }, leftUpperArm: { z: 1.4 }, rightUpperArm: { z: -1.4 }, leftLowerArm: { z: 0 }, rightLowerArm: { z: 0 } } }
            ]
        },

        // 说话时的轻微动作
        talking: {
            duration: 2000,
            loop: true,
            keyframes: [
                { time: 0, bones: { head: { x: 0, y: 0, z: 0 }, rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 } } },
                { time: 500, bones: { head: { x: 0.015, y: 0.015, z: 0 }, rightUpperArm: { z: -1.45 }, rightLowerArm: { x: -0.1 } } },
                { time: 1000, bones: { head: { x: -0.015, y: -0.015, z: 0.01 }, rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 } } },
                { time: 1500, bones: { head: { x: 0.01, y: 0.01, z: -0.01 }, rightUpperArm: { z: -1.45 }, rightLowerArm: { x: -0.1 } } },
                { time: 2000, bones: { head: { x: 0, y: 0, z: 0 }, rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 } } }
            ]
        },

        // 招手
        waveHand: {
            duration: 1500,
            loop: false,
            keyframes: [
                { time: 0, bones: { rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } },
                { time: 300, bones: { rightUpperArm: { z: -2.8, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: 0 } } },
                { time: 500, bones: { rightUpperArm: { z: -2.8, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: 0.4 } } },
                { time: 700, bones: { rightUpperArm: { z: -2.8, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: -0.4 } } },
                { time: 900, bones: { rightUpperArm: { z: -2.8, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: 0.4 } } },
                { time: 1100, bones: { rightUpperArm: { z: -2.8, x: -0.2 }, rightLowerArm: { x: -0.2 }, rightHand: { z: -0.4 } } },
                { time: 1300, bones: { rightUpperArm: { z: -2.0, x: -0.1 }, rightLowerArm: { x: -0.1 }, rightHand: { z: 0 } } },
                { time: 1500, bones: { rightUpperArm: { z: -1.4 }, rightLowerArm: { z: 0 }, rightHand: { z: 0 } } }
            ]
        },

        // 鼓掌
        clap: {
            duration: 1200,
            loop: false,
            keyframes: [
                { time: 0, bones: { leftUpperArm: { z: 0.873 }, rightUpperArm: { z: -0.873 }, leftLowerArm: { x: -0.3 }, rightLowerArm: { x: -0.3 } } },
                { time: 200, bones: { leftUpperArm: { z: 1.5, x: -0.5 }, rightUpperArm: { z: -1.5, x: -0.5 }, leftLowerArm: { x: -1.2 }, rightLowerArm: { x: -1.2 } } },
                { time: 400, bones: { leftUpperArm: { z: 1.4, x: -0.5 }, rightUpperArm: { z: -1.4, x: -0.5 }, leftLowerArm: { x: -1.0 }, rightLowerArm: { x: -1.0 } } },
                { time: 600, bones: { leftUpperArm: { z: 1.5, x: -0.5 }, rightUpperArm: { z: -1.5, x: -0.5 }, leftLowerArm: { x: -1.2 }, rightLowerArm: { x: -1.2 } } },
                { time: 800, bones: { leftUpperArm: { z: 1.4, x: -0.5 }, rightUpperArm: { z: -1.4, x: -0.5 }, leftLowerArm: { x: -1.0 }, rightLowerArm: { x: -1.0 } } },
                { time: 1000, bones: { leftUpperArm: { z: 1.2, x: -0.3 }, rightUpperArm: { z: -1.2, x: -0.3 }, leftLowerArm: { x: -0.6 }, rightLowerArm: { x: -0.6 } } },
                { time: 1200, bones: { leftUpperArm: { z: 0.873 }, rightUpperArm: { z: -0.873 }, leftLowerArm: { x: -0.3 }, rightLowerArm: { x: -0.3 } } }
            ]
        }
    },

    // 缓动函数
    easing: {
        linear: t => t,
        easeInQuad: t => t * t,
        easeOutQuad: t => t * (2 - t),
        easeInOutQuad: t => t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t,
        easeInCubic: t => t * t * t,
        easeOutCubic: t => (--t) * t * t + 1,
        easeInOutCubic: t => t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1,
        easeOutElastic: t => {
            const p = 0.3;
            return Math.pow(2, -10 * t) * Math.sin((t - p / 4) * (2 * Math.PI) / p) + 1;
        }
    },

    // 当前动画状态
    currentAnimation: null,
    animationStartTime: 0,
    isPlaying: false,
    onComplete: null,

    /**
     * 播放动画
     * @param {string} animationName - 动画名称
     * @param {Object} vrmModel - VRM模型
     * @param {Function} onComplete - 完成回调
     * @param {string} easingType - 缓动类型
     */
    play(animationName, vrmModel, onComplete = null, easingType = 'easeInOutCubic') {
        const animation = this.animations[animationName];
        if (!animation) {
            console.warn(`[VRMAnimation] Animation not found: ${animationName}`);
            return;
        }

        this.currentAnimation = animation;
        this.animationStartTime = Date.now();
        this.isPlaying = true;
        this.onComplete = onComplete;
        this.currentEasing = this.easing[easingType] || this.easing.easeInOutCubic;

        console.log(`[VRMAnimation] Playing: ${animationName}`);
    },

    /**
     * 停止动画
     */
    stop() {
        this.isPlaying = false;
        this.currentAnimation = null;
    },

    /**
     * 更新动画（需要在动画循环中调用）
     * @param {Object} vrmModel - VRM模型
     */
    update(vrmModel) {
        if (!this.isPlaying || !this.currentAnimation || !vrmModel || !vrmModel.humanoid) {
            return;
        }

        const now = Date.now();
        const elapsed = now - this.animationStartTime;
        const duration = this.currentAnimation.duration;
        let progress = elapsed / duration;

        // 处理循环
        if (this.currentAnimation.loop) {
            progress = progress % 1;
        } else if (progress >= 1) {
            progress = 1;
            this.isPlaying = false;
        }

        // 应用缓动
        const easedProgress = this.currentEasing(progress);

        // 找到当前关键帧
        const keyframes = this.currentAnimation.keyframes;
        let prevFrame = keyframes[0];
        let nextFrame = keyframes[keyframes.length - 1];

        for (let i = 0; i < keyframes.length - 1; i++) {
            const currentTime = keyframes[i].time / duration;
            const nextTime = keyframes[i + 1].time / duration;

            if (easedProgress >= currentTime && easedProgress <= nextTime) {
                prevFrame = keyframes[i];
                nextFrame = keyframes[i + 1];
                break;
            }
        }

        // 计算帧间插值
        const prevTime = prevFrame.time / duration;
        const nextTime = nextFrame.time / duration;
        const frameProgress = (easedProgress - prevTime) / (nextTime - prevTime);
        const t = Math.max(0, Math.min(1, frameProgress));

        // 应用骨骼变换
        const humanoid = vrmModel.humanoid;
        for (const boneName in prevFrame.bones) {
            const bone = humanoid.getNormalizedBoneNode(boneName);
            if (!bone) continue;

            const prevValues = prevFrame.bones[boneName];
            const nextValues = nextFrame.bones[boneName] || prevValues;

            for (const axis in prevValues) {
                const prevVal = prevValues[axis];
                const nextVal = nextValues[axis] !== undefined ? nextValues[axis] : prevVal;
                bone.rotation[axis] = prevVal + (nextVal - prevVal) * t;
            }
        }

        // 动画完成回调
        if (!this.isPlaying && this.onComplete) {
            this.onComplete();
            this.onComplete = null;
        }
    },

    /**
     * 获取所有可用动画名称
     */
    getAnimationNames() {
        return Object.keys(this.animations);
    },

    /**
     * 添加自定义动画
     * @param {string} name - 动画名称
     * @param {Object} animation - 动画定义
     */
    addAnimation(name, animation) {
        this.animations[name] = animation;
    }
};

// 导出到全局
window.VRMAnimation = VRMAnimation;
