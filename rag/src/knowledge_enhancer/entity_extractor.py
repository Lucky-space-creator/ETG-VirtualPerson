import re
from typing import List, Dict


class EntityExtractor:
    """NER 实体抽取（规则 + 词典匹配，景区特化）

    生产环境可替换为：
        - 百度 LAC / HanLP / spaCy-zh
        - 大模型 NER（ChatGPT Function Calling）
    """

    PATTERNS = {
        "person": [
            r"([\u4e00-\u9fff]{2,4})(?:大师|法师|皇帝|皇后|太子|公主|将军|丞相|太守|县令|住持|方丈|道士|和尚|僧人|禅师)",
            r"([\u4e00-\u9fff]{2,3})(?:主持|修建|建造|重修|修缮|扩建|设计|创建|开山)(?:大|佛|殿|塔|寺|庙)",
            r"(?:玄奘|鉴真|慧能|达摩|济公|唐僧|如来|观音|文殊|普贤|地藏)",
        ],
        "time": [
            r"((?:贞观|开元|万历|康熙|乾隆|光绪|天宝|永乐|宣德|正德|嘉靖|崇祯)\s*\d{1,2}年)",
            r"(\d{4}年)",
            r"([唐宋元明清][\u4e00-\u9fff]{0,4}(?:年间|时期|朝代|至今))",
        ],
        "building": [
            r"([\u4e00-\u9fff]{2,4}(?:殿|阁|楼|塔|寺|庙|祠|堂|院|亭|台|桥|门|宫|坛|廊|坊|窟|庵|洞))(?!代|年)",
            r"(?:大雄宝殿|藏经楼|钟楼|鼓楼|山门|牌坊|经幢|佛塔|舍利塔|灵山大佛|梵宫|五印坛城|九龙灌浴|大照壁|佛手广场|祥符禅寺|曼飞龙塔)",
            r"([\u4e00-\u9fff]{2,4}(?:大佛|佛像|佛))",
        ],
        "event": [
            r"([\u4e00-\u9fff]{2,10}(?:重修|扩建|修缮|翻修|新建|烧毁|战争|起义|变法|法会|庙会|开光))",
            r"(?:佛教论坛|水陆法会|浴佛节|腊八节)",
        ],
        "location": [
            r"([\u4e00-\u9fff]{2,6}(?:山|河|湖|海|省|市|县|村|镇|区|园|景区))",
            r"((?:太湖|西湖|洞庭湖|鄱阳湖|青海湖|滇池|洱海))",
        ],
        "scenery": [
            r"([\u4e00-\u9fff]{2,8}(?:八景|十景|胜境|圣地|道场|福地|洞天))",
            r"(?:[^\s]{2,6}(?:景观|景点|遗迹|古迹|石刻|壁画|雕塑|佛像|浮雕))",
        ],
        "price": [
            r"(\d{1,3}元/[人张次套车座位])",
            r"((?:门票|套票|联票|半价|老人票|学生票|儿童票|免票))",
        ],
    }

    def extract(self, text: str) -> Dict:
        """提取实体

        Returns:
            {"entities": [...], "relations": []}
        """
        entities = []
        for entity_type, patterns in self.PATTERNS.items():
            for pattern in patterns:
                try:
                    for match in re.finditer(pattern, text):
                        name = match.group(1) if match.lastindex else match.group(0)
                        entities.append({
                            "type": entity_type,
                            "name": name,
                            "start": match.start(),
                            "end": match.end(),
                        })
                except Exception:
                    continue

        # 去重
        seen = set()
        unique = []
        for e in entities:
            key = (e["type"], e["name"])
            if key not in seen:
                seen.add(key)
                unique.append(e)

        return {"entities": unique, "relations": []}
