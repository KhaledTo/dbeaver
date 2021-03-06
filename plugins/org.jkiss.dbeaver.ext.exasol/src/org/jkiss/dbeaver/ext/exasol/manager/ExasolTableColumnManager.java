package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableBase;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Exasol Table Column Manager
 * 
 * @author Karl Griesser
 * 
 */
public class ExasolTableColumnManager extends SQLTableColumnManager<ExasolTableColumn, ExasolTableBase> implements DBEObjectRenamer<ExasolTableColumn> {

    private static final String SQL_ALTER = "ALTER TABLE %s MODIFY COLUMN %s ";
    private static final String SQL_COMMENT = "COMMENT ON COLUMN %s.%s IS '%s'";


    private static final String CMD_ALTER = "Alter Column";
    private static final String CMD_COMMENT = "Comment on Column";


    // -----------------
    // Business Contract
    // -----------------
    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, ExasolTableColumn> getObjectsCache(ExasolTableColumn object)
    {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache((ExasolTable) object.getParentObject());
    }

    @Override
    public boolean canEditObject(ExasolTableColumn object)
    {
        // Edit is only availabe for ExasolTable and not for other kinds of tables (View, MQTs, Nicknames..)
        ExasolTableBase exasolTableBase = object.getParentObject();
        if ((exasolTableBase != null) & (exasolTableBase.getClass().equals(ExasolTable.class))) {
            return true;
        } else {
            return false;
        }
    }

    // ------
    // Create
    // ------

    @Override
    protected ExasolTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, ExasolTableBase parent,
                                                  Object copyFrom)
    {
        ExasolTableColumn column = new ExasolTableColumn(parent);
        column.setName(getNewColumnName(monitor, context, parent));
        return column;
    }

    // -----
    // Alter
    // -----
    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        ExasolTableColumn exasolColumn = command.getObject();

        if (!command.getProperties().isEmpty()) {
        	final String deltaSQL = exasolColumn.getName() +  " " + exasolColumn.getFormatType() 
        		+ " " + (exasolColumn.getDefaultValue() == null ? "" : " DEFAULT " + exasolColumn.getDefaultValue()) 
        		+ " " + (exasolColumn.isIdentity() ? " IDENTITY " + exasolColumn.getIdentityValue().toString() : "") 
        		+ " " + (exasolColumn.isRequired() ? "NOT NULL" : "NULL") 
        		; 
            if (!deltaSQL.isEmpty()) {
                String sqlAlterColumn = String.format(SQL_ALTER, exasolColumn.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL), deltaSQL);
                actionList.add(new SQLDatabasePersistAction(CMD_ALTER, sqlAlterColumn));
            }
        }

        // Comment
        DBEPersistAction commentAction = buildCommentAction(exasolColumn);
        if (commentAction != null) {
            actionList.add(commentAction);
        }

    }

    // -------
    // Helpers
    // -------
    private DBEPersistAction buildCommentAction(ExasolTableColumn exasolColumn)
    {
        if (CommonUtils.isNotEmpty(exasolColumn.getDescription())) {
            String tableName = exasolColumn.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL);
            String columnName = exasolColumn.getName();
            String comment = exasolColumn.getDescription();
            String commentSQL = String.format(SQL_COMMENT, tableName, columnName, comment);
            return new SQLDatabasePersistAction(CMD_COMMENT, commentSQL);
        } else {
            return null;
        }
    }


    @Override
    public void renameObject(DBECommandContext commandContext, ExasolTableColumn object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command)
    {
        final ExasolTableColumn column = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " RENAME COLUMN " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName()))
        );
    }
}
