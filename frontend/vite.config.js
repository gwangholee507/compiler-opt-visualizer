import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 프론트엔드는 백엔드를 항상 같은 출처의 /api 로 호출한다 (App.jsx).
    // 배포된 jar에서는 백엔드가 화면과 API를 함께 내보내므로 그대로 동작하고,
    // 개발 서버(5173)에서는 /api 요청만 백엔드(8080)로 넘겨준다.
    // localhost 대신 127.0.0.1을 쓰는 이유: 백엔드는 127.0.0.1에서만 듣는데,
    // 환경에 따라 localhost가 IPv6(::1)로 먼저 풀려 연결이 거부될 수 있다.
    proxy: {
      '/api': 'http://127.0.0.1:8080',
    },
  },
})
