import { h } from 'vue'
import StoryWrapper from './StoryWrapper.vue'

export const withVuetifyTheme = (storyFn, context) => {
  const story = storyFn()

  return () => {
    return h(
      StoryWrapper,
      {},
      {
        story: () => h(story, { ...context.args }),
      },
    )
  }
}
