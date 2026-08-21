/**
 * 두 줄 배열(base, target)을 LCS(최장 공통 부분 수열) 기반으로 비교합니다.
 * target의 각 줄이 base에도 "같은 순서로" 존재하면 'same', 아니면 'added'로 표시하고,
 * base에만 있고 target에는 없는 줄의 개수를 removedCount로 셉니다.
 *
 * 어셈블리 파일은 보통 수백 줄 이내라 O(n*m) DP로도 충분히 빠릅니다.
 */
export function diffLines(base, target) {
  const n = base.length
  const m = target.length

  // dp[i][j] = base[i..] 와 target[j..] 의 LCS 길이
  const dp = Array.from({ length: n + 1 }, () => new Array(m + 1).fill(0))
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      dp[i][j] =
        base[i] === target[j]
          ? dp[i + 1][j + 1] + 1
          : Math.max(dp[i + 1][j], dp[i][j + 1])
    }
  }

  const lineStates = [] // target 각 줄에 대응: 'same' | 'added'
  let removedCount = 0
  let i = 0
  let j = 0
  while (i < n && j < m) {
    if (base[i] === target[j]) {
      lineStates.push('same')
      i++
      j++
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      removedCount++
      i++
    } else {
      lineStates.push('added')
      j++
    }
  }
  removedCount += n - i
  while (j < m) {
    lineStates.push('added')
    j++
  }

  const addedCount = lineStates.filter((s) => s === 'added').length
  return { lineStates, addedCount, removedCount }
}
