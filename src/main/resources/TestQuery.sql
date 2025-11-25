-- ###########################################################
-- 1. 전역 설정 및 헬퍼 테이블 생성
-- ###########################################################

-- 트랜잭션 시작 (대량 삽입 성능을 위해 트랜잭션으로 묶습니다)
START TRANSACTION;

-- [A] 데이터 생성을 위한 헬퍼 테이블 생성 (0~9)
CREATE TABLE IF NOT EXISTS helper_numbers (i INT NOT NULL PRIMARY KEY);
TRUNCATE TABLE helper_numbers;
INSERT INTO helper_numbers (i) VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

-- [B] 2배수 곱셈용 헬퍼 테이블 생성
CREATE TABLE IF NOT EXISTS helper_mult (i INT NOT NULL PRIMARY KEY);
TRUNCATE TABLE helper_mult;
INSERT INTO helper_mult (i) VALUES (1), (2);


-- ###########################################################
-- 2. 외래 키 제약 조건 선행 데이터 삽입 (Members, Sellers, Venue)
-- ###########################################################

-- 2-1. [SELLERS] 100명의 판매자 생성
INSERT INTO sellers (seller_name, seller_email, seller_type, seller_phone_number, seller_register_date)
SELECT
    CONCAT('Seller_', (t1.i + t2.i * 10) + 1),
    CONCAT('seller_', (t1.i + t2.i * 10) + 1, '@event.com'),
    'CONCERT',
    '111-222-3333',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY)
FROM
    helper_numbers AS t1
        CROSS JOIN
    helper_numbers AS t2
    LIMIT 100; -- ID 1 ~ 100번까지의 Seller 생성

-- 2-2. [MEMBERS] 1,000명의 회원 생성 (orders 테이블 FK 보장)
INSERT INTO members (email, phone_number, password, name)
SELECT
    CONCAT('user_', (t1.i + t2.i * 10 + t3.i * 100) + 1, '@test.com'),
    CONCAT('010', (t1.i + t2.i * 10 + t3.i * 100) + 1),
    'dummy_hash',
    CONCAT('테스트회원', (t1.i + t2.i * 10 + t3.i * 100) + 1)
FROM
    helper_numbers AS t1
        CROSS JOIN
    helper_numbers AS t2
        CROSS JOIN
    helper_numbers AS t3
    LIMIT 1000; -- ID 1 ~ 1000번까지의 Member 생성

-- 2-3. [VENUE] 1개의 장소 생성 (events 테이블 FK 보장)
INSERT INTO venues (venue_name, capacity, venue_phone_number)
VALUES ('테스트 주경기장', 60000, '02-1234-5678'); -- ID 1번 Venue 생성


-- ###########################################################
-- 3. 핵심 대용량 데이터 삽입
-- ###########################################################

-- 3-1. [EVENTS] 10만 건의 이벤트 생성 (Seller, Venue FK 참조)
INSERT INTO events (venue_id, seller_id, event_name, event_type, start_time, end_time, max_ticket_count, current_ticket_stock_count)
SELECT
    1, -- venue_id 1번 고정
    (ABS(CAST(RAND() * 100 AS SIGNED)) + 1), -- 랜덤 seller ID (1-100)
    CONCAT('Event Title ', t1.i + t2.i * 10 + t3.i * 100 + t4.i * 1000 + t5.i * 10000),
    CASE WHEN RAND() < 0.5 THEN 'CONCERT' ELSE 'OTHER' END,
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 YEAR), INTERVAL FLOOR(RAND() * 31536000) SECOND) AS start_time,
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 YEAR), INTERVAL FLOOR(RAND() * 31536000) SECOND) AS end_time,
    FLOOR(100 + (RAND() * 1000)),
    FLOOR(100 + (RAND() * 1000))
FROM
    helper_numbers AS t1
        CROSS JOIN helper_numbers AS t2
        CROSS JOIN helper_numbers AS t3
        CROSS JOIN helper_numbers AS t4
        CROSS JOIN helper_numbers AS t5
    LIMIT 100000; -- 10만 건 삽입

-- 3-2. [ORDERS] 100만 건의 주문 생성 (Member FK 참조)
INSERT INTO orders (member_id, ordered_at, total_order_amount, status)
SELECT
    (ABS(CAST(RAND() * 1000 AS SIGNED)) + 1), -- 랜덤 member ID (1-1000)
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 YEAR), INTERVAL FLOOR(RAND() * 31536000) SECOND),
    FLOOR(10000 + (RAND() * 100000)),
    CASE WHEN RAND() < 0.9 THEN 'SUCCESS' ELSE 'CANCELED' END
FROM
    helper_numbers AS t1
        CROSS JOIN helper_numbers AS t2
        CROSS JOIN helper_numbers AS t3
        CROSS JOIN helper_numbers AS t4
        CROSS JOIN helper_numbers AS t5
        CROSS JOIN helper_numbers AS t6
        CROSS JOIN helper_numbers AS t7
    LIMIT 1000000; -- 100만 건 삽입

-- 3-3. [TICKETS] 100만 건의 개별 티켓 생성 (Event, Seller FK 참조)
-- ⚠️ events 테이블에 100,000건이 존재한다고 가정
INSERT INTO tickets (event_id, seller_id, seat_info, ticket_price, status)
SELECT
    (ABS(CAST(RAND() * 100000 AS SIGNED)) + 1), -- 랜덤 event_id (1-100k)
    (ABS(CAST(RAND() * 100 AS SIGNED)) + 1), -- 랜덤 seller_id (1-100)
    CONCAT('Seat_', i),
    FLOOR(50000 + (RAND() * 100000)),
    CASE WHEN RAND() < 0.8 THEN 'AVAILABLE' ELSE 'SOLDOUT' END
FROM (
         SELECT @rn := @rn + 1 AS i FROM
             (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
             (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
             (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t3,
             (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t4,
             (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t5,
             (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t6,
             (SELECT @rn := 0) vars
     ) AS numbers
    LIMIT 1000000; -- 100만 건 삽입

-- 3-4. [ORDERTICKET] 200만 건의 주문 상세 생성 (Order, Ticket FK 참조)
-- ⚠️ orders 및 tickets 테이블 ID 1~1M까지 존재한다고 가정
INSERT INTO order_ticket (order_id, ticket_id, created_at, purchase_price)
SELECT
    t_order.order_id,
    (ABS(CAST(RAND() * 1000000 AS SIGNED)) + 1) AS random_ticket_id, -- 랜덤 ticket ID (1-1M)
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 YEAR), INTERVAL FLOOR(RAND() * 31536000) SECOND),
    FLOOR(10000 + (RAND() * 100000))
FROM
    orders t_order
        CROSS JOIN
    helper_mult h_mult
    LIMIT 2000000; -- 200만 건 삽입

-- 4. 트랜잭션 커밋
COMMIT;

-- 5. 임시 테이블 삭제 (정리)
DROP TABLE helper_numbers;
DROP TABLE helper_mult;