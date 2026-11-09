# SpawnerShop 1.0.0

Cửa hàng bán spawner thật của SmartSpawner (gộp stack, giao diện bấm chuột phải như Donut).
Trừ tiền qua Vault (EssentialsX), sau đó chạy lệnh `ss give spawner <player> <type> <amount>`.

- Lệnh: `/spawnershop` (bí danh `/sps`, `/spawners`), `/spawnershop reload` (quyền `spawnershop.admin`)
- Mở cửa hàng: quyền `spawnershop.use` (mặc định ai cũng có)
- Chuột trái: mua 1. Shift + chuột trái: mua 10 (chỉnh trong config).

## Build

Cần JDK 21 và Maven: `mvn clean package` -> lấy `target/SpawnerShop.jar` bỏ vào `plugins/`.
Hoặc đẩy lên GitHub, tab **Actions** sẽ tự build ra file jar.

## Chỉnh

Mọi thứ nằm trong `plugins/SpawnerShop/config.yml`: tiêu đề, giá, danh sách spawner, tin nhắn, lệnh give.
Sau khi sửa gõ `/spawnershop reload`.
