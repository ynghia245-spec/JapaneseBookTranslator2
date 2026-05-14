# Japanese Book Translator - Hướng dẫn dành cho người không biết code

App này giúp bạn:
1. **Chụp ảnh** trang sách tiếng Nhật bằng camera điện thoại
2. **Tự động nhận diện** chữ Nhật (OCR) bằng Google ML Kit (offline, miễn phí)
3. **Tự động dịch** sang tiếng Việt (offline, miễn phí)
4. **Xuất file PDF** chứa toàn bộ ảnh gốc + bản dịch tiếng Việt

Mọi xử lý đều diễn ra trên điện thoại của bạn — không cần gửi sách lên server nào, không tốn data sau lần tải model đầu tiên.

---

## Cách lấy file APK (cài vào điện thoại) — DÀNH CHO BẠN

Vì bạn không biết code, bạn sẽ dùng dịch vụ build APK miễn phí của GitHub. Quy trình một lần duy nhất, sau đó chỉ cần tải file APK về.

### Bước 1 — Tạo tài khoản GitHub miễn phí

Vào https://github.com → bấm **Sign up** → đăng ký bằng email (chỉ cần 2 phút).

### Bước 2 — Upload source code lên GitHub

**Cách dễ nhất (không cần git):**

1. Vào https://github.com/new
2. Đặt tên repo bất kỳ, ví dụ `JapaneseBookTranslator`
3. Chọn **Public** (nếu muốn dùng GitHub Actions miễn phí)
4. Bấm **Create repository**
5. Trong repo vừa tạo, bấm **uploading an existing file** (chữ xanh)
6. Kéo-thả **toàn bộ thư mục** `JapaneseBookTranslator` mà mình đã tạo (kể cả thư mục `.github` ẩn) — hoặc zip lại rồi upload và GitHub sẽ tự bung ra
7. Bấm **Commit changes**

### Bước 3 — Chờ GitHub build APK tự động (~5–10 phút)

1. Trong repo, bấm tab **Actions** (ở thanh ngang trên cùng)
2. Bạn sẽ thấy job "Build Android APK" đang chạy (chấm vàng quay)
3. Đợi khi nào chuyển thành dấu tích xanh ✓

### Bước 4 — Tải file APK về

1. Bấm vào job đã build xong
2. Cuộn xuống cuối, mục **Artifacts** sẽ có file `JapaneseBookTranslator-debug-apk.zip`
3. Bấm tải về máy tính
4. Giải nén, bạn sẽ có file `app-debug.apk`
5. Copy file `.apk` này sang điện thoại Android (qua USB, Zalo, email, Google Drive, v.v.)

### Bước 5 — Cài APK lên điện thoại Android

1. Trên điện thoại, mở file `app-debug.apk` vừa copy sang
2. Lần đầu cài, Android sẽ hỏi "Cài từ nguồn không xác định" → bật cho ứng dụng vừa mở (thường là File Manager hoặc Chrome)
3. Bấm **Cài đặt**
4. Mở app **Japanese Book Translator** trong launcher

---

## Cách dùng app

1. Mở app → bấm **Chụp trang sách**
2. Cho phép quyền **Camera** khi được hỏi
3. Đưa camera vào trang sách → bấm nút tròn ở giữa để chụp
4. App tự động OCR + dịch (mất 2–5 giây/trang)
   - **Lần đầu** sẽ tải model dịch (~30MB) — cần Wi-Fi
   - Sau đó dùng được hoàn toàn offline
5. Lặp lại cho từng trang sách
6. Khi xong, quay về màn hình chính → bấm **Xuất PDF tiếng Việt**
7. File PDF sẽ tự mở (mỗi spread = 1 trang ảnh gốc + 1 trang bản dịch tiếng Việt)
8. File lưu tại: `Internal Storage/Android/data/com.nghia.jptranslator/files/Documents/`

---

## Mẹo để OCR chính xác hơn

- **Ánh sáng tốt**, tránh bóng đổ
- Để **trang sách thẳng**, không bị cong/lệch
- Giữ điện thoại **song song với mặt sách**, không nghiêng
- Với sách kỹ thuật có chữ nhỏ, chụp **gần hơn** và chỉ 1 cột/lần

---

## Giới hạn cần biết

- **Bản dịch tự động (ML Kit)** chưa hoàn hảo với sách kỹ thuật/học thuật — thuật ngữ chuyên ngành có thể sai. Đây là giới hạn của model dịch máy offline, không phải của app.
- App **không tái tạo layout gốc** (không phải vì khó code, mà vì OCR ML Kit không trả về font/màu/vị trí pixel chính xác). PDF xuất ra là "ảnh gốc + bản dịch dạng text".
- Với sách 150+ trang, hãy chụp theo từng chương và xuất PDF từng phần để tránh app bị nặng.

---

## Phương án thay thế (nếu bạn ngại setup GitHub)

Nếu bạn không muốn làm theo các bước trên, có 2 app sẵn trên Play Store gần tương đương:
- **Google Translate (Google Dịch)** — chế độ Camera dịch Nhật-Việt rất tốt
- **Google Lens** — tương tự, kèm copy text

Hai app này không xuất PDF được, nhưng nếu chỉ cần đọc-hiểu nội dung sách thì đủ dùng.
