import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusBadge from './StatusBadge.vue'

describe('StatusBadge', () => {
  it('renders a human label and status-specific class', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'RETRY_PENDING' } })

    expect(wrapper.text()).toContain('Reintento')
    expect(wrapper.classes()).toContain('status-retry_pending')
  })
})
