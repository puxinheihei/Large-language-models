-- 初始化用户表
CREATE TABLE IF NOT EXISTS users (
                                     id CHAR(36) PRIMARY KEY,
                                     username VARCHAR(100) NOT NULL UNIQUE,
                                     email VARCHAR(255),
                                     password VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化行程总览表（含总预算与人数）
CREATE TABLE IF NOT EXISTS itineraries (
                                           id CHAR(36) PRIMARY KEY,
                                           user_id CHAR(36) NULL,
                                           destination VARCHAR(255) NOT NULL,
                                           start_date DATE NOT NULL,
                                           days INT NOT NULL,
                                           budget DECIMAL(12,2) NULL,
                                           people_count INT NULL,
                                           preferences TEXT NULL,
                                           summary VARCHAR(500) NULL,
                                           created_at DATETIME NOT NULL,
                                           INDEX idx_itin_user (user_id),
                                           INDEX idx_itin_created (created_at),
                                           CONSTRAINT fk_itin_user FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化行程每日表（含日预算）
CREATE TABLE IF NOT EXISTS itinerary_days (
                                              id CHAR(36) PRIMARY KEY,
                                              itinerary_id CHAR(36) NOT NULL,
                                              day_index INT NOT NULL,
                                              summary VARCHAR(500),
                                              daily_budget DECIMAL(12,2) NULL,
                                              INDEX idx_day_itin (itinerary_id),
                                              INDEX idx_day_index (day_index),
                                              CONSTRAINT fk_day_itin FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化每日地点表
CREATE TABLE IF NOT EXISTS itinerary_places (
                                                id CHAR(36) PRIMARY KEY,
                                                day_id CHAR(36) NOT NULL,
                                                name VARCHAR(255) NOT NULL,
                                                type VARCHAR(100),
                                                address VARCHAR(500),
                                                lat DOUBLE,
                                                lng DOUBLE,
                                                notes VARCHAR(500),
                                                INDEX idx_place_day (day_id),
                                                CONSTRAINT fk_place_day FOREIGN KEY (day_id) REFERENCES itinerary_days(id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化预算记录表（含行程关联）
CREATE TABLE IF NOT EXISTS budget_records (
                                              id CHAR(36) PRIMARY KEY,
                                              category VARCHAR(100) NOT NULL,
                                              amount DECIMAL(12,2) NOT NULL,
                                              description VARCHAR(500),
                                              date DATE NOT NULL,
                                              itinerary_id CHAR(36) NULL,
                                              INDEX idx_budget_date (date),
                                              INDEX idx_budget_category (category),
                                              INDEX idx_budget_itinerary (itinerary_id),
                                              CONSTRAINT fk_budget_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;