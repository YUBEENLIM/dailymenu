/// 카테고리 칩 목록 (한글 표시 ↔ 백엔드 enum 1:1 매핑)
/// 백엔드 MenuCategory enum 13개 중 사용자 선택 대상만 노출.
/// 제외: CAFE(KakaoPlaceAdapter EXCLUDED_KEYWORDS로 도달 불가), OTHER(분류 미상)
const categoryChips = [
  {'label': '한식', 'value': 'KOREAN'},
  {'label': '일식', 'value': 'JAPANESE'},
  {'label': '중식', 'value': 'CHINESE'},
  {'label': '양식', 'value': 'WESTERN'},
  {'label': '패스트푸드', 'value': 'FAST_FOOD'},
  {'label': '아시안', 'value': 'ASIAN'},
  // 4/24 PR #30로 추가된 카테고리
  {'label': '분식', 'value': 'BUNSIK'},
  {'label': '샐러드', 'value': 'SALAD'},
  {'label': '고기', 'value': 'MEAT'},
  {'label': '피자', 'value': 'PIZZA'},
];

const enumToLabel = {
  'KOREAN': '한식',
  'CHINESE': '중식',
  'JAPANESE': '일식',
  'WESTERN': '양식',
  'FAST_FOOD': '패스트푸드',
  'ASIAN': '아시안',
  'CAFE': '카페/디저트',
  'CHICKEN': '치킨',
  // 4/24 PR #30로 추가된 카테고리
  'BUNSIK': '분식',
  'SALAD': '샐러드',
  'MEAT': '고기',
  'PIZZA': '피자',
  'OTHER': '기타',
};

String categoryLabel(String enumValue) => enumToLabel[enumValue] ?? enumValue;
