export const categories = [
  { id: 1, label: 'Spor Ayakkabi' },
  { id: 2, label: 'Sneaker' },
  { id: 3, label: 'Telefon' },
  { id: 4, label: 'Tablet' },
  { id: 5, label: 'Moda' },
  { id: 6, label: 'Ev Mobilya' },
  { id: 7, label: 'Mutfak' },
  { id: 8, label: 'Temizlik' },
  { id: 9, label: 'Kisisel Bakim' },
  { id: 10, label: 'Market' }
];

export function categoryLabel(categoryId) {
  const found = categories.find((c) => c.id === Number(categoryId));
  return found ? found.label : `Category #${categoryId}`;
}
