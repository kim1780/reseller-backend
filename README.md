# 🛒 Reseller Backend

ระบบ Backend สำหรับแพลตฟอร์มตัวแทนจำหน่าย (Reseller Platform)  
พัฒนาด้วย **Spring Boot** รองรับการจัดการสินค้า ออเดอร์ ตัวแทน และกระเป๋าเงิน

---

## 🧱 Tech Stack

- **Java 17+** + **Spring Boot**
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL** 
- **Lombok** (optional)
- **Maven**

---

## 📁 โครงสร้างโปรเจกต์
```
src/
├── admin/
│   ├── controller/       # API endpoints สำหรับ Admin
│   ├── dto/              # Request/Response DTOs
│   ├── entity/           # Entity เฉพาะฝั่ง Admin
│   ├── repository/       # JPA Repositories ฝั่ง Admin
│   └── service/          # Business logic ฝั่ง Admin
├── controller/           # API endpoints ฝั่ง Reseller/Customer
├── entity/               # Entity หลัก (User, Orders, Wallet, Shop ฯลฯ)
├── repository/           # JPA Repositories หลัก
└── ResellerBackendApplication.java
```

---

## ✨ ฟีเจอร์หลัก

### 👤 Admin
- จัดการสินค้า (เพิ่ม / แก้ไข / ลบ / ค้นหา)
- อนุมัติหรือระงับตัวแทน (reseller)
- จัดการออเดอร์ (จัดส่ง / ปิดออเดอร์)
- Dashboard สรุปยอดขาย กำไร และสถิติตัวแทน

### 🏪 Reseller
- สมัครและจัดการร้านค้าของตัวเอง
- ดูสินค้า ตั้งราคาขาย และจัดการออเดอร์
- ดูยอดกระเป๋าเงิน (Wallet) และประวัติการได้รับกำไร

### 💰 Wallet
- บันทึกกำไรอัตโนมัติเมื่อออเดอร์ถูกจัดส่ง
- ประวัติรายการผ่าน WalletLog

---

## 🚀 วิธีรันโปรเจกต์

### 1. Clone โปรเจกต์
```bash
git clone https://github.com/kim1780/reseller-backend.git
cd reseller-backend
```

### 2. ตั้งค่า Database
แก้ไขไฟล์ `src/main/resources/application.properties`
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/reseller_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

### 3. รันโปรเจกต์
```bash
mvn spring-boot:run
```

Server จะรันที่ `http://localhost:8080`

---

## 📡 API Endpoints (ตัวอย่าง)

| Method | Endpoint | คำอธิบาย |
|--------|----------|----------|
| GET | `/api/admin/dashboard` | ดึงข้อมูล Dashboard |
| GET | `/api/admin/products` | ดึงรายการสินค้าทั้งหมด |
| POST | `/api/admin/products` | เพิ่มสินค้าใหม่ |
| PUT | `/api/admin/products/{id}` | แก้ไขสินค้า |
| DELETE | `/api/admin/products/{id}` | ลบสินค้า |
| GET | `/api/admin/orders` | ดึงรายการออเดอร์ |
| POST | `/api/admin/orders/{id}/ship` | จัดส่งออเดอร์ |
| POST | `/api/admin/orders/{id}/complete` | ปิดออเดอร์ |
| PUT | `/api/admin/resellers/{id}/status` | อัปเดตสถานะตัวแทน |

---

## 👨‍💻 ผู้พัฒนา

พัฒนาโดย **[Poramin Dueanphen]**
