/**
 * @jest-environment jsdom
 */

import {screen} from '@testing-library/vue';
import makeMe from '../fixtures/makeMe';
import helper from '../helpers';
import NoteRealm from "@/components/notes/views/NoteRealm.vue";

describe('note realm overview', () => {

  beforeEach(() => {
    helper.reset();
  });

  it('should render reply-input', async () => {
    const note = makeMe.aNoteRealm.title('single note').please();
    helper.store.loadNoteRealms([note]);
    helper.component(NoteRealm).withProps({
      noteId: note.id,
      expandChildren: false
    }).render();
    const byTestId = await screen.getByTestId('reply-input');
    expect(byTestId).toHaveAttribute('placeholder', 'Reply...');
  });
});
