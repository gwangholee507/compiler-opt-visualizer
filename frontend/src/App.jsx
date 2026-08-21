import { useState } from 'react'
import './App.css'

const DEFAULT_CODE = `int square(int x) {
    return x * x;
}

int sum_loop(int n) {
    int total = 0;
    for (int i = 0; i < n; i++) {
        total += square(i);
    }
    return total;
}
`

const API_BASE = 'http://localhost:8080'

function formatBytes(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return `${bytes} B`
  return `${(bytes / 1024).toFixed(1)} KB`
}

function OptimizationColumn({ result }) {
  return (
    <div className="opt-column">
      <div className="opt-header">
        <h3>-{result.level}</h3>
        {result.success && (
          <span className="opt-meta">
            {formatBytes(result.binarySizeBytes)} · {result.compileTimeMs}ms
          </span>
        )}
      </div>
      {result.success ? (
        <pre className="asm-block">{result.assembly}</pre>
      ) : (
        <pre className="asm-block error">{result.errorMessage}</pre>
      )}
    </div>
  )
}

function App() {
  const [code, setCode] = useState(DEFAULT_CODE)
  const [results, setResults] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function handleCompare() {
    setLoading(true)
    setError(null)
    setResults(null)
    try {
      const res = await fetch(`${API_BASE}/api/compile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code }),
      })
      if (!res.ok) {
        const body = await res.json().catch(() => null)
        throw new Error(body?.message || `요청 실패 (HTTP ${res.status})`)
      }
      const data = await res.json()
      setResults(data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <header>
        <h1>컴파일러 최적화 옵션 비교기</h1>
        <p className="subtitle">
          같은 C 코드를 -O0 ~ -O3 로 컴파일해서 어셈블리와 바이너리 크기를 비교합니다.
        </p>
      </header>

      <section className="editor-section">
        <textarea
          className="code-editor"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          spellCheck={false}
        />
        <button onClick={handleCompare} disabled={loading}>
          {loading ? '컴파일 중...' : '비교하기'}
        </button>
        {error && <p className="error-message">⚠ {error}</p>}
      </section>

      {results && (
        <section className="results-section">
          {results.map((r) => (
            <OptimizationColumn key={r.level} result={r} />
          ))}
        </section>
      )}
    </div>
  )
}

export default App
