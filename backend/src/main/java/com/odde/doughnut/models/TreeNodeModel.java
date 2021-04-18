package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;

public class TreeNodeModel extends ModelForEntity<Note> {
    protected final NoteRepository noteRepository;

    public TreeNodeModel(Note note, ModelFactoryService modelFactoryService) {
        super(note, modelFactoryService);
        this.noteRepository = modelFactoryService.noteRepository;
    }

    public void destroy() {
        entity.traverseBreadthFirst(child ->
                modelFactoryService.toTreeNodeModel(child).destroy());
        modelFactoryService.reviewPointRepository.deleteAllByNote(getEntity());
        if (entity.getNotebook() != null) {
            if (entity.getNotebook().getHeadNote() == entity) {
                modelFactoryService.notebookRepository.delete(entity.getNotebook());
            }
        }
        modelFactoryService.noteRepository.delete(getEntity());
    }

}
