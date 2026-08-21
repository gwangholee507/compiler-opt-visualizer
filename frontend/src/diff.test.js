import { describe, it, expect } from 'vitest'
import { diffLines } from './diff'

describe('diffLines', () => {
  it('완전히 동일한 두 배열은 모두 same, added/removed 0개', () => {
    const lines = ['mov w0, w0', 'ret']
    const { lineStates, addedCount, removedCount } = diffLines(lines, lines)

    expect(lineStates).toEqual(['same', 'same'])
    expect(addedCount).toBe(0)
    expect(removedCount).toBe(0)
  })

  it('target에만 있는 줄은 added로 표시된다', () => {
    const base = ['a', 'b']
    const target = ['a', 'b', 'c']

    const { lineStates, addedCount, removedCount } = diffLines(base, target)

    expect(lineStates).toEqual(['same', 'same', 'added'])
    expect(addedCount).toBe(1)
    expect(removedCount).toBe(0)
  })

  it('base에만 있는 줄은 removedCount로 집계된다 (target에는 나타나지 않음)', () => {
    const base = ['a', 'b', 'c']
    const target = ['a', 'c']

    const { lineStates, addedCount, removedCount } = diffLines(base, target)

    // target 기준으로만 상태를 매기므로 lineStates 길이는 target과 같다
    expect(lineStates).toEqual(['same', 'same'])
    expect(addedCount).toBe(0)
    expect(removedCount).toBe(1)
  })

  it('O0 -> O1 최적화로 줄어드는 실제 시나리오를 흉내낸다', () => {
    const o0 = [
      'sub sp, sp, #16',
      'str w0, [sp, #12]',
      'ldr w8, [sp, #12]',
      'ldr w9, [sp, #12]',
      'mul w0, w8, w9',
      'add sp, sp, #16',
      'ret',
    ]
    const o1 = ['mul w0, w0, w0', 'ret']

    const { lineStates, addedCount, removedCount } = diffLines(o0, o1)

    // o1의 'mul w0, w0, w0'는 o0에 없는 새 줄, 'ret'는 공통
    expect(lineStates).toEqual(['added', 'same'])
    expect(addedCount).toBe(1)
    expect(removedCount).toBe(6)
  })

  it('빈 배열끼리는 아무 변화도 없다', () => {
    const { lineStates, addedCount, removedCount } = diffLines([], [])
    expect(lineStates).toEqual([])
    expect(addedCount).toBe(0)
    expect(removedCount).toBe(0)
  })

  it('base가 비어있으면 target의 모든 줄이 added', () => {
    const { lineStates, addedCount, removedCount } = diffLines([], ['x', 'y'])
    expect(lineStates).toEqual(['added', 'added'])
    expect(addedCount).toBe(2)
    expect(removedCount).toBe(0)
  })

  it('target이 비어있으면 removedCount는 base 전체 길이', () => {
    const { lineStates, addedCount, removedCount } = diffLines(['x', 'y'], [])
    expect(lineStates).toEqual([])
    expect(addedCount).toBe(0)
    expect(removedCount).toBe(2)
  })
})
