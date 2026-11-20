CREATE TABLE products (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name char(255) NOT NULL UNIQUE,
    price bigint NOT NULL DEFAULT 0,
    category char(50) NOT NULL,
    in_stock boolean NOT NULL DEFAULT true,
    date_of_incomming date NOT NULL DEFAULT now()::date
);

SELECT name, price FROM products
WHERE category = 'Электроника' AND in_stock = true;

UPDATE products SET price = price * 1.1
WHERE category = 'Книги';

INSERT INTO products (name, price, category, in_stock)
VALUES ('Моя первая PostgreSQL книга', 500, 'Книги', true);

DELETE FROM products WHERE in_stock = false;

UPDATE products
SET
    price = price * 0.8,
    name = '(Скидочная позиция) '|| TRIM(name)
WHERE
    category = 'Книги'
    AND date_of_incomming BETWEEN (now() - INTERVAL '2 years')::date
    AND (now() - INTERVAL '6 months')::date;