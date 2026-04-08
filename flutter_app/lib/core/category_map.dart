/// 카테고리 칩 목록 (한글 표시 ↔ 백엔드 enum 1:1 매핑)
const categoryChips = [
  {'label': '한식', 'value': 'KOREAN'},
  {'label': '일식', 'value': 'JAPANESE'},
  {'label': '중식', 'value': 'CHINESE'},
  {'label': '양식', 'value': 'WESTERN'},
  {'label': '패스트푸드', 'value': 'FAST_FOOD'},
  {'label': '아시안', 'value': 'ASIAN'},
];

const enumToLabel = {
  'KOREAN': '한식',
  'CHINESE': '중식',
  'JAPANESE': '일식',
  'WESTERN': '양식',
  'FAST_FOOD': '패스트푸드',
  'ASIAN': '아시안',
  'CAFE': '카페/디저트',
  'OTHER': '기타',
};

String categoryLabel(String enumValue) => enumToLabel[enumValue] ?? enumValue;
