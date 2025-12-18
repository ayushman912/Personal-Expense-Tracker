-- Sample Data for Personal Expense Tracker
USE expense_tracker;

-- Insert default user
INSERT INTO users (username, password, email) VALUES
('admin', 'admin123', 'admin@example.com'),
('user1', 'password', 'user1@example.com');

-- Insert sample categories
INSERT INTO categories (name, type, description) VALUES
-- Expense categories
('Food & Dining', 'EXPENSE', 'Restaurants, groceries, and food expenses'),
('Transportation', 'EXPENSE', 'Gas, public transport, car maintenance'),
('Shopping', 'EXPENSE', 'Clothing, electronics, general shopping'),
('Bills & Utilities', 'EXPENSE', 'Electricity, water, internet, phone bills'),
('Entertainment', 'EXPENSE', 'Movies, games, subscriptions'),
-- Income categories
('Salary', 'INCOME', 'Monthly salary income'),
('Freelance', 'INCOME', 'Freelance work income'),
('Investment', 'INCOME', 'Investment returns and dividends'),
('Gift', 'INCOME', 'Gifts received'),
('Other Income', 'INCOME', 'Other sources of income');

-- Insert sample transactions
INSERT INTO transactions (amount, description, date, category_id, type) VALUES
-- Expenses
(45.50, 'Grocery shopping at Walmart', '2024-01-15', 1, 'Expense'),
(25.00, 'Lunch at restaurant', '2024-01-16', 1, 'Expense'),
(60.00, 'Gas for car', '2024-01-17', 2, 'Expense'),
(120.00, 'New shoes', '2024-01-18', 3, 'Expense'),
(80.00, 'Electricity bill', '2024-01-20', 4, 'Expense'),
(15.99, 'Netflix subscription', '2024-01-21', 5, 'Expense'),
(35.00, 'Dinner with friends', '2024-01-22', 1, 'Expense'),
(200.00, 'Monthly rent', '2024-01-01', 4, 'Expense'),
(50.00, 'Uber rides', '2024-01-19', 2, 'Expense'),
(89.99, 'Amazon purchase', '2024-01-23', 3, 'Expense'),
-- Income
(3500.00, 'Monthly salary', '2024-01-01', 6, 'Income'),
(500.00, 'Freelance web design project', '2024-01-10', 7, 'Income'),
(150.00, 'Stock dividend', '2024-01-15', 8, 'Income'),
(100.00, 'Birthday gift', '2024-01-20', 9, 'Income'),
(200.00, 'Part-time job', '2024-01-25', 7, 'Income');

