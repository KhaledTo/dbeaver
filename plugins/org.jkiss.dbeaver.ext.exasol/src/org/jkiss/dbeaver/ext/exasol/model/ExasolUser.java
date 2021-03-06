package org.jkiss.dbeaver.ext.exasol.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolUser implements DBAUser, DBPRefreshableObject, DBPSaveableObject {

	
	private static final Log log = Log.getLog(ExasolUser.class);
	
	private ExasolDataSource dataSource;
	private String userName;
	private String description;
    private boolean persisted;
	
    private List<ExasolRole> roles;
	
	public ExasolUser(ExasolDataSource dataSource, ResultSet resultSet)
	{
		this.dataSource = dataSource;
		if (resultSet != null)
		{
			this.persisted = true;
			this.userName = JDBCUtils.safeGetString(resultSet, "USER_NAME");
			this.description = JDBCUtils.safeGetString(resultSet, "USER_COMMENT");
		}
		else 
		{
			this.persisted = false;
			this.userName = "user";
			this.description = "";
		}
	}
	
	@Override
	@Property(viewable = true, order = 100)
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public DBSObject getParentObject() {
		return this.dataSource.getContainer();
	}

	@Override
	public DBPDataSource getDataSource() {
		return this.dataSource;
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 1)
	public String getName() {
		return this.userName;
	}

	@Override
	public boolean isPersisted() {
		return this.persisted;
	}

	@Override
	public void setPersisted(boolean persisted) {
		this.persisted = persisted;
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		roles = null;
		return this;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<ExasolRole> getRoles(DBRProgressMonitor monitor) throws DBException
	{
		if (this.roles != null)
		{
			return this.roles;
		}
		if (! isPersisted())
		{
			this.roles = new ArrayList();
			return this.roles;
		}
		
		try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read User Roles"))
		{
            try (JDBCPreparedStatement dbStat = session.prepareStatement("select * from EXA_USER_ROLE_PRIVS")) {
            	try (JDBCResultSet dbResult = dbStat.executeQuery())
            	{
                	this.roles = new ArrayList();
                	while (dbResult.next())
                	{
                		this.roles.add(new ExasolRole(dataSource, dbResult));
                	}
            		return this.roles;
            	}
            	
            } catch (SQLException e) {
            	throw new DBException(e,getDataSource());
			}
		}
		
		
	}
	

}
