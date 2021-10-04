import Builder from "./Builder"
import ReviewPointBuilder from "./ReviewPointBuilder";

class RepetitionBuilder extends Builder {
  data: any;
  note: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.note = null
  }

  ofNote(note: any): RepetitionBuilder {
    this.note = note
    return this
  }

  do(): any {
    return {
        reviewPointViewedByUser: new ReviewPointBuilder().ofNote(this.note).do(),
        quizQuestion: {
          questionType: "CLOZE_SELECTION",
          options: [
            {
              note: {
                id: 1,
                notePicture: null,
                head: true,
                noteTypeDisplay: "Child Note",
                title: "question",
                shortDescription: "answer",
              },
              picture: false,
              display: "question",
            },
          ],
          description: "answer",
          mainTopic: "",
          pictureQuestion: false,
        },
    }
  }
}

export default RepetitionBuilder