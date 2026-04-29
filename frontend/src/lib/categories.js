export const categories = [
  { id: 1, label: 'Sports Shoes' },
  { id: 2, label: 'Sneakers' },
  { id: 3, label: 'Phones' },
  { id: 4, label: 'Tablets' },
  { id: 5, label: 'Fashion' },
  { id: 6, label: 'Home Furniture' },
  { id: 7, label: 'Kitchen' },
  { id: 8, label: 'Cleaning' },
  { id: 9, label: 'Beauty' },
  { id: 10, label: 'Grocery' }
];

export function categoryLabel(categoryId) {
  const found = categories.find((c) => c.id === Number(categoryId));
  return found ? found.label : `Category #${categoryId}`;
}
