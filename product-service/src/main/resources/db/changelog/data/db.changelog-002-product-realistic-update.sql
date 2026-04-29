--liquibase formatted sql

--changeset codex:product-realistic-002
UPDATE products
SET
    category_id = ((id - 1) / 20) + 1,
    seller_id = ((id - 1) / 40) + 1,
    brand = CASE ((id - 1) / 20) + 1
        WHEN 1 THEN 'Nike'
        WHEN 2 THEN 'Adidas'
        WHEN 3 THEN 'Apple'
        WHEN 4 THEN 'Samsung'
        WHEN 5 THEN 'Mudo'
        WHEN 6 THEN 'IKEA'
        WHEN 7 THEN 'Philips'
        WHEN 8 THEN 'Bosch'
        WHEN 9 THEN 'Maybelline'
        ELSE 'Nestle'
    END,
    name = CASE ((id - 1) / 20) + 1
        WHEN 1 THEN 'Running Shoes Model ' || lpad(id::text, 3, '0')
        WHEN 2 THEN 'Sneaker Street ' || lpad(id::text, 3, '0')
        WHEN 3 THEN 'Smartphone X' || lpad(id::text, 3, '0')
        WHEN 4 THEN 'Tablet Pro ' || lpad(id::text, 3, '0')
        WHEN 5 THEN 'Cotton T-Shirt ' || lpad(id::text, 3, '0')
        WHEN 6 THEN 'Dining Chair Set ' || lpad(id::text, 3, '0')
        WHEN 7 THEN 'Air Fryer ' || lpad(id::text, 3, '0')
        WHEN 8 THEN 'Robot Vacuum ' || lpad(id::text, 3, '0')
        WHEN 9 THEN 'Skin Care Serum ' || lpad(id::text, 3, '0')
        ELSE 'Organic Coffee Beans ' || lpad(id::text, 3, '0')
    END,
    description = CASE ((id - 1) / 20) + 1
        WHEN 1 THEN 'Lightweight and breathable running shoes for daily training and city walks.'
        WHEN 2 THEN 'Street style sneakers with soft sole and all-day comfort.'
        WHEN 3 THEN 'High-performance smartphone with OLED display and long battery life.'
        WHEN 4 THEN 'Slim tablet for media, note taking and remote work.'
        WHEN 5 THEN 'Everyday basic cotton t-shirt with regular fit.'
        WHEN 6 THEN 'Modern dining chair set designed for compact living spaces.'
        WHEN 7 THEN 'Large capacity air fryer with multiple cooking presets.'
        WHEN 8 THEN 'Smart robot vacuum with mapping and scheduled cleaning.'
        WHEN 9 THEN 'Hydrating skin serum for daily care routine.'
        ELSE 'Medium roast whole bean coffee with rich aroma.'
    END,
    badge_type = CASE
        WHEN id % 17 = 0 THEN 'HOT_DEAL'
        WHEN id % 11 = 0 THEN 'BEST_SELLER'
        ELSE NULL
    END;
