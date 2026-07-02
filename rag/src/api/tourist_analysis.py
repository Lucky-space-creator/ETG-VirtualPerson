"""
游客消费行为分析API
"""
from fastapi import APIRouter, UploadFile, File, HTTPException
from typing import Optional, List
from pydantic import BaseModel
import pandas as pd
import io
import json

router = APIRouter()


class TouristDataSummary(BaseModel):
    """游客数据摘要"""
    total_records: int
    date_range: str
    attractions: List[str]
    age_stats: dict
    gender_stats: dict
    cost_stats: dict
    satisfaction_stats: dict


class ConsumptionPattern(BaseModel):
    """消费模式"""
    pattern_name: str
    description: str
    percentage: float
    avg_cost: float
    characteristics: List[str]


class AnalysisReport(BaseModel):
    """分析报告"""
    summary: TouristDataSummary
    patterns: List[ConsumptionPattern]
    insights: List[str]
    recommendations: List[str]


@router.post("/analyze")
async def analyze_tourist_data(file: UploadFile = File(...)):
    """分析游客消费数据Excel文件"""
    try:
        # 读取Excel文件
        content = await file.read()
        df = pd.read_excel(io.BytesIO(content))

        # 基础统计
        total_records = len(df)
        date_range = f"{df['visit_date'].min()} 至 {df['visit_date'].max()}"
        attractions = df['attraction_name'].unique().tolist()

        # 年龄统计
        age_stats = {
            "mean": float(df['age'].mean()),
            "min": int(df['age'].min()),
            "max": int(df['age'].max()),
            "std": float(df['age'].std()),
            "distribution": {
                "18-25": int(len(df[df['age'].between(18, 25)])),
                "26-35": int(len(df[df['age'].between(26, 35)])),
                "36-45": int(len(df[df['age'].between(36, 45)])),
                "46-55": int(len(df[df['age'].between(46, 55)])),
                "56+": int(len(df[df['age'] >= 56]))
            }
        }

        # 性别统计
        gender_stats = df['gender'].value_counts().to_dict()

        # 消费统计
        cost_cols = ['ticket_cost', 'food_cost', 'shopping_cost', 'transport_cost', 'entertainment_cost', 'total_cost']
        cost_stats = {}
        for col in cost_cols:
            cost_stats[col] = {
                "mean": float(df[col].mean()),
                "min": float(df[col].min()),
                "max": float(df[col].max()),
                "median": float(df[col].median())
            }

        # 消费结构分析
        total_avg = df['total_cost'].mean()
        cost_structure = {
            "ticket_ratio": float(df['ticket_cost'].sum() / df['total_cost'].sum() * 100),
            "food_ratio": float(df['food_cost'].sum() / df['total_cost'].sum() * 100),
            "shopping_ratio": float(df['shopping_cost'].sum() / df['total_cost'].sum() * 100),
            "transport_ratio": float(df['transport_cost'].sum() / df['total_cost'].sum() * 100),
            "entertainment_ratio": float(df['entertainment_cost'].sum() / df['total_cost'].sum() * 100)
        }

        # 满意度统计
        satisfaction_stats = df['satisfaction'].value_counts().to_dict()
        avg_satisfaction = float(df['satisfaction'].mean())

        # 按年龄段分析消费
        age_consumption = {}
        for age_group, (low, high) in [("18-25", (18, 25)), ("26-35", (26, 35)), ("36-45", (36, 45)), ("46-55", (46, 55)), ("56+", (56, 100))]:
            if high == 100:
                group_df = df[df['age'] >= low]
            else:
                group_df = df[df['age'].between(low, high)]
            if len(group_df) > 0:
                age_consumption[age_group] = {
                    "count": int(len(group_df)),
                    "avg_total_cost": float(group_df['total_cost'].mean()),
                    "avg_stay_duration": float(group_df['stay_duration'].mean()),
                    "avg_satisfaction": float(group_df['satisfaction'].mean())
                }

        # 按景点分析消费
        attraction_consumption = {}
        for attr in attractions[:10]:  # 只取前10个景点
            attr_df = df[df['attraction_name'] == attr]
            attraction_consumption[attr] = {
                "count": int(len(attr_df)),
                "avg_total_cost": float(attr_df['total_cost'].mean()),
                "avg_stay_duration": float(attr_df['stay_duration'].mean()),
                "avg_satisfaction": float(attr_df['satisfaction'].mean())
            }

        # 消费模式识别
        patterns = identify_consumption_patterns(df)

        # 生成洞察
        insights = generate_insights(df, cost_structure, age_consumption)

        # 生成建议
        recommendations = generate_recommendations(df, cost_structure, patterns)

        return {
            "status": "ok",
            "summary": {
                "total_records": total_records,
                "date_range": date_range,
                "attractions_count": len(attractions),
                "attractions": attractions[:20],  # 只返回前20个
                "age_stats": age_stats,
                "gender_stats": gender_stats,
                "cost_stats": cost_stats,
                "cost_structure": cost_structure,
                "satisfaction_stats": satisfaction_stats,
                "avg_satisfaction": avg_satisfaction
            },
            "age_consumption": age_consumption,
            "attraction_consumption": attraction_consumption,
            "patterns": patterns,
            "insights": insights,
            "recommendations": recommendations
        }

    except Exception as e:
        raise HTTPException(500, f"数据分析失败: {str(e)}")


def identify_consumption_patterns(df: pd.DataFrame) -> List[dict]:
    """识别消费模式"""
    patterns = []

    # 模式1：高消费游客
    high_cost_threshold = df['total_cost'].quantile(0.75)
    high_cost_df = df[df['total_cost'] >= high_cost_threshold]
    if len(high_cost_df) > 0:
        patterns.append({
            "pattern_name": "高消费游客",
            "description": f"总消费 >= {high_cost_threshold:.0f}元",
            "percentage": float(len(high_cost_df) / len(df) * 100),
            "avg_cost": float(high_cost_df['total_cost'].mean()),
            "characteristics": [
                f"平均年龄: {high_cost_df['age'].mean():.1f}岁",
                f"平均停留: {high_cost_df['stay_duration'].mean():.1f}小时",
                f"平均满意度: {high_cost_df['satisfaction'].mean():.1f}"
            ]
        })

    # 模式2：低消费游客
    low_cost_threshold = df['total_cost'].quantile(0.25)
    low_cost_df = df[df['total_cost'] <= low_cost_threshold]
    if len(low_cost_df) > 0:
        patterns.append({
            "pattern_name": "低消费游客",
            "description": f"总消费 <= {low_cost_threshold:.0f}元",
            "percentage": float(len(low_cost_df) / len(df) * 100),
            "avg_cost": float(low_cost_df['total_cost'].mean()),
            "characteristics": [
                f"平均年龄: {low_cost_df['age'].mean():.1f}岁",
                f"平均停留: {low_cost_df['stay_duration'].mean():.1f}小时",
                f"平均满意度: {low_cost_df['satisfaction'].mean():.1f}"
            ]
        })

    # 模式3：购物主导型
    shopping_dominant = df[df['shopping_cost'] > df['total_cost'] * 0.4]
    if len(shopping_dominant) > 0:
        patterns.append({
            "pattern_name": "购物主导型",
            "description": "购物消费占比超过40%",
            "percentage": float(len(shopping_dominant) / len(df) * 100),
            "avg_cost": float(shopping_dominant['total_cost'].mean()),
            "characteristics": [
                f"平均购物消费: {shopping_dominant['shopping_cost'].mean():.0f}元",
                f"购物占比: {(shopping_dominant['shopping_cost'].sum() / shopping_dominant['total_cost'].sum() * 100):.1f}%"
            ]
        })

    # 模式4：餐饮主导型
    food_dominant = df[df['food_cost'] > df['total_cost'] * 0.35]
    if len(food_dominant) > 0:
        patterns.append({
            "pattern_name": "餐饮主导型",
            "description": "餐饮消费占比超过35%",
            "percentage": float(len(food_dominant) / len(df) * 100),
            "avg_cost": float(food_dominant['total_cost'].mean()),
            "characteristics": [
                f"平均餐饮消费: {food_dominant['food_cost'].mean():.0f}元",
                f"餐饮占比: {(food_dominant['food_cost'].sum() / food_dominant['total_cost'].sum() * 100):.1f}%"
            ]
        })

    # 模式5：家庭出游型
    family_group = df[df['group_size'] >= 3]
    if len(family_group) > 0:
        patterns.append({
            "pattern_name": "家庭出游型",
            "description": "3人及以上团体",
            "percentage": float(len(family_group) / len(df) * 100),
            "avg_cost": float(family_group['total_cost'].mean()),
            "characteristics": [
                f"平均团体大小: {family_group['group_size'].mean():.1f}人",
                f"人均消费: {(family_group['total_cost'].sum() / family_group['group_size'].sum()):.0f}元"
            ]
        })

    return patterns


def generate_insights(df: pd.DataFrame, cost_structure: dict, age_consumption: dict) -> List[str]:
    """生成数据洞察"""
    insights = []

    # 消费结构洞察
    max_category = max(cost_structure.items(), key=lambda x: x[1])
    category_names = {
        "ticket_ratio": "门票",
        "food_ratio": "餐饮",
        "shopping_ratio": "购物",
        "transport_ratio": "交通",
        "entertainment_ratio": "娱乐"
    }
    insights.append(f"消费结构中，{category_names.get(max_category[0], max_category[0])}占比最高，达到{max_category[1]:.1f}%")

    # 年龄消费洞察
    if age_consumption:
        highest_age_group = max(age_consumption.items(), key=lambda x: x[1]['avg_total_cost'])
        insights.append(f"{highest_age_group[0]}岁年龄段消费能力最强，平均消费{highest_age_group[1]['avg_total_cost']:.0f}元")

    # 满意度洞察
    avg_satisfaction = df['satisfaction'].mean()
    if avg_satisfaction >= 4:
        insights.append(f"整体满意度较高（{avg_satisfaction:.2f}），游客体验良好")
    elif avg_satisfaction >= 3:
        insights.append(f"整体满意度中等（{avg_satisfaction:.2f}），有提升空间")
    else:
        insights.append(f"整体满意度较低（{avg_satisfaction:.2f}），需要重点关注")

    # 停留时间洞察
    avg_stay = df['stay_duration'].mean()
    insights.append(f"平均停留时间为{avg_stay:.1f}小时，建议优化景区动线设计")

    # 团体规模洞察
    avg_group = df['group_size'].mean()
    insights.append(f"平均团体规模为{avg_group:.1f}人，家庭和小团体是主要客群")

    return insights


def generate_recommendations(df: pd.DataFrame, cost_structure: dict, patterns: List[dict]) -> List[str]:
    """生成优化建议"""
    recommendations = []

    # 基于消费结构的建议
    if cost_structure.get('shopping_ratio', 0) < 20:
        recommendations.append("购物消费占比较低，建议增加特色商品开发和购物点布局")

    if cost_structure.get('food_ratio', 0) > 30:
        recommendations.append("餐饮消费占比较高，可考虑引入更多特色餐饮品牌")

    # 基于满意度的建议
    low_satisfaction = df[df['satisfaction'] <= 2]
    if len(low_satisfaction) / len(df) > 0.1:
        recommendations.append(f"有{len(low_satisfaction)/len(df)*100:.1f}%的游客满意度较低，需调查原因并改进")

    # 基于消费模式的建议
    for pattern in patterns:
        if pattern['pattern_name'] == '高消费游客' and pattern['percentage'] < 20:
            recommendations.append("高消费游客比例较低，建议开发高端旅游产品")

        if pattern['pattern_name'] == '家庭出游型' and pattern['percentage'] > 30:
            recommendations.append("家庭游客比例较高，建议增加亲子互动项目")

    # 通用建议
    recommendations.append("建议建立会员体系，提高游客复购率")
    recommendations.append("可考虑推出淡季优惠活动，平衡客流分布")

    return recommendations


@router.post("/analyze/llm")
async def analyze_with_llm(file: UploadFile = File(...)):
    """使用LLM深度分析游客消费数据"""
    try:
        # 先进行基础分析
        content = await file.read()
        df = pd.read_excel(io.BytesIO(content))

        # 准备数据摘要给LLM
        data_summary = prepare_data_summary(df)

        # 调用LLM进行分析
        from src.config import LLM_API_URL, LLM_API_KEY, LLM_MODEL

        import httpx

        prompt = f"""你是一个旅游行业数据分析专家。请根据以下游客消费数据，生成一份详细的消费习惯分析报告。

数据摘要：
{data_summary}

请从以下角度分析：
1. 消费结构特点
2. 不同年龄段消费差异
3. 消费与满意度的关系
4. 潜在的消费趋势
5. 针对性的营销建议

请用中文输出，格式清晰，内容专业。"""

        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(
                f"{LLM_API_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {LLM_API_KEY}",
                    "Content-Type": "application/json"
                },
                json={
                    "model": LLM_MODEL,
                    "messages": [
                        {"role": "system", "content": "你是一个专业的旅游行业数据分析师。"},
                        {"role": "user", "content": prompt}
                    ],
                    "max_tokens": 2000,
                    "temperature": 0.7
                }
            )

            if response.status_code == 200:
                result = response.json()
                llm_analysis = result['choices'][0]['message']['content']
            else:
                llm_analysis = "LLM分析暂时不可用，请稍后重试"

        return {
            "status": "ok",
            "llm_analysis": llm_analysis
        }

    except Exception as e:
        raise HTTPException(500, f"LLM分析失败: {str(e)}")


def prepare_data_summary(df: pd.DataFrame) -> str:
    """准备数据摘要给LLM"""
    summary = f"""
数据集包含 {len(df)} 条游客记录。
时间范围: {df['visit_date'].min()} 至 {df['visit_date'].max()}
景点数量: {df['attraction_name'].nunique()} 个

年龄分布:
- 平均年龄: {df['age'].mean():.1f}岁
- 年龄范围: {df['age'].min()}-{df['age'].max()}岁

性别分布:
- 男性: {len(df[df['gender']=='男'])} 人 ({len(df[df['gender']=='男'])/len(df)*100:.1f}%)
- 女性: {len(df[df['gender']=='女'])} 人 ({len(df[df['gender']=='女'])/len(df)*100:.1f}%)

消费统计:
- 平均总消费: {df['total_cost'].mean():.2f}元
- 门票消费: {df['ticket_cost'].mean():.2f}元 (占比{df['ticket_cost'].sum()/df['total_cost'].sum()*100:.1f}%)
- 餐饮消费: {df['food_cost'].mean():.2f}元 (占比{df['food_cost'].sum()/df['total_cost'].sum()*100:.1f}%)
- 购物消费: {df['shopping_cost'].mean():.2f}元 (占比{df['shopping_cost'].sum()/df['total_cost'].sum()*100:.1f}%)
- 交通消费: {df['transport_cost'].mean():.2f}元 (占比{df['transport_cost'].sum()/df['total_cost'].sum()*100:.1f}%)
- 娱乐消费: {df['entertainment_cost'].mean():.2f}元 (占比{df['entertainment_cost'].sum()/df['total_cost'].sum()*100:.1f}%)

满意度:
- 平均满意度: {df['satisfaction'].mean():.2f} (满分5分)
- 满意度分布: {df['satisfaction'].value_counts().to_dict()}

停留时间:
- 平均停留: {df['stay_duration'].mean():.1f}小时

团体规模:
- 平均团体: {df['group_size'].mean():.1f}人

热门景点 (前5):
{df['attraction_name'].value_counts().head(5).to_string()}
"""
    return summary
