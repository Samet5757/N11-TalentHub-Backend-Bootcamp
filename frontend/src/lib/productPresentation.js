const categoryImageTags = {
  1: 'running-shoes',
  2: 'sneakers',
  3: 'smartphone',
  4: 'tablet',
  5: 'fashion',
  6: 'furniture',
  7: 'kitchen',
  8: 'cleaning',
  9: 'skincare',
  10: 'groceries'
};

const categoryNamePools = {
  1: ['Performans Kosu Ayakkabisi', 'Nefes Alan Spor Ayakkabi', 'Hafif Kosu Ayakkabisi'],
  2: ['Gundelik Sneaker', 'Street Sneaker', 'Konfor Sneaker'],
  3: ['Akilli Telefon', 'Yuksek Performansli Telefon', 'Gelismis Kamera Telefonu'],
  4: ['Multimedya Tablet', 'Ince Tasarim Tablet', 'Egitim ve Is Tableti'],
  5: ['Sezonluk Moda Urunu', 'Gunluk Stil Urunu', 'Modern Tasarim Urunu'],
  6: ['Modern Salon Mobilyasi', 'Konfor Odakli Mobilya', 'Minimal Ev Mobilyasi'],
  7: ['Profesyonel Mutfak Urunu', 'Dayanikli Mutfak Gereci', 'Pratik Mutfak Yardimcisi'],
  8: ['Hijyen Temizlik Urunu', 'Cok Amacli Temizlik Urunu', 'Ev Temizlik Cozumu'],
  9: ['Cilt Bakim Urunu', 'Kisisel Bakim Cozumu', 'Guzellik ve Bakim Urunu'],
  10: ['Temel Market Urunu', 'Gunluk Tuketim Urunu', 'Aile Paket Urunu']
};

function pickName(product) {
  const categoryId = Number(product?.categoryId);
  const pool = categoryNamePools[categoryId] || ['Urun'];
  const index = Math.abs(Number(product?.id || 0)) % pool.length;
  const base = pool[index];
  return product?.brand ? `${product.brand} ${base}` : base;
}

function pickImage(product) {
  const categoryId = Number(product?.categoryId);
  const tag = categoryImageTags[categoryId] || 'product';
  const lock = Math.abs(Number(product?.id || categoryId || 1));
  return `https://picsum.photos/seed/${encodeURIComponent(`${tag}-${lock}`)}/960/640`;
}

export function presentProduct(product) {
  if (!product) return product;
  return {
    ...product,
    displayName: pickName(product),
    displayImageUrl: pickImage(product)
  };
}
