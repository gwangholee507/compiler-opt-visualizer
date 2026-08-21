import { useState } from 'react'
import './App.css'
import { diffLines } from './diff'

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

function AssemblyView({ assembly, baseAssembly }) {
  const lines = assembly.split('\n')

  // 기준(O0)이 없거나 자기 자신이 기준이면 그냥 평문으로 표시
  if (!baseAssembly) {
    return (
      <pre className="asm-block">
        {lines.map((line, idx) => (
          <div key={idx} className="asm-line">
            {line}
          </div>
        ))}
      </pre>
    )
  }

  const { lineStates } = diffLines(baseAssembly.split('\n'), lines)

  return (
    <pre className="asm-block">
      {lines.map((line, idx) => (
        <div
          key={idx}
          className={lineStates[idx] === 'added' ? 'asm-line diff-added' : 'asm-line'}
        >
          {line}
        </div>
      ))}
    </pre>
  )
}

function OptimizationColumn({ result, baseAssembly }) {
  const isBaseline = result.level === 'O0'
  const diffSummary =
    !isBaseline && result.success && baseAssembly
      ? diffLines(baseAssembly.split('\n'), result.assembly.split('\n'))
      : null

  return (
    <div className="opt-column">
      <div className="opt-header">
        <h3>-{result.level}</h3>
        {result.success && (
          <span className="opt-meta">
            {formatBytes(result.binarySizeBytes)} · {result.compileTimeMs}ms
            {diffSummary && (
              <span className="diff-badge">
                {' '}
                (+{diffSummary.addedCount} / -{diffSummary.removedCount} vs O0)
              </span>
            )}
          </span>
        )}
      </div>
      {result.success ? (
        <AssemblyView
          assembly={result.assembly}
          baseAssembly={isBaseline ? null : baseAssembly}
        />
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
          {(() => {
            const base = results.find((r) => r.level === 'O0')
            const baseAssembly = base?.success ? base.assembly : null
            return results.map((r) => (
              <OptimizationColumn key={r.level} result={r} baseAssembly={baseAssembly} />
            ))
          })()}
        </section>
      )}
    </div>
  )
}

export default App
