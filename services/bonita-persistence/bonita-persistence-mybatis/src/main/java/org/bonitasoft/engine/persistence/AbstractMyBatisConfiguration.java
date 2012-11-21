/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.persistence;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Mesta
 */
public class AbstractMyBatisConfiguration {

    private final Map<String, String> typeAliases;

    private final List<String> mappers;

    private final Map<String, Long> sequencesMappings;

    private final Map<String, String> classAliasMappings;

    private final Set<StatementMapping> statementMapping;

    private final Map<String, String> dbStatementsMapping;

    private final Map<String, String> entityMappings;

    private final Map<String, String> classFieldAliasMappings;

    public AbstractMyBatisConfiguration(final Map<String, String> typeAliases, final List<String> mappers, final Map<String, Long> sequencesMappings,
            final Map<String, String> classAliasMappings, final Map<String, String> classFieldAliasMappings, final Set<StatementMapping> statementMapping,
            final Map<String, String> dbStatementsMapping, final Map<String, String> entityMappings) {
        super();
        this.typeAliases = typeAliases;
        this.mappers = mappers;
        this.sequencesMappings = sequencesMappings;
        this.classAliasMappings = classAliasMappings;
        this.classFieldAliasMappings = classFieldAliasMappings;
        this.statementMapping = statementMapping;
        this.dbStatementsMapping = dbStatementsMapping;
        this.entityMappings = entityMappings;
    }

    public Map<String, String> getDbStatementsMapping() {
        if (dbStatementsMapping == null) {
            return Collections.emptyMap();
        }
        return dbStatementsMapping;
    }

    public Map<String, Long> getSequencesMappings() {
        if (sequencesMappings == null) {
            return Collections.emptyMap();
        }
        return sequencesMappings;
    }

    public Map<String, String> getClassAliasMappings() {
        if (classAliasMappings == null) {
            return Collections.emptyMap();
        }
        return classAliasMappings;
    }

    public Set<StatementMapping> getStatementMapping() {
        if (statementMapping == null) {
            return Collections.emptySet();
        }
        return statementMapping;
    }

    public Map<String, StatementMapping> getStatementMappings() {
        final Map<String, StatementMapping> statementMappings = new HashMap<String, StatementMapping>(getStatementMapping().size());
        for (final StatementMapping statementMapping : getStatementMapping()) {
            statementMappings.put(statementMapping.getSourceStatement(), statementMapping);
        }
        return statementMappings;
    }

    public Map<String, String> getTypeAliases() {
        if (typeAliases == null) {
            return Collections.emptyMap();
        }
        return typeAliases;
    }

    public List<String> getMappers() {
        if (mappers == null) {
            return Collections.emptyList();
        }
        return mappers;
    }

    public Map<String, String> getEntityMappings() {
        if (entityMappings == null) {
            return Collections.emptyMap();
        }
        return entityMappings;
    }

    public Map<? extends String, ? extends String> getClassFieldAliasMappings() {
        if (classFieldAliasMappings == null) {
            return Collections.emptyMap();
        }
        return classFieldAliasMappings;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (classAliasMappings == null ? 0 : classAliasMappings.hashCode());
        result = prime * result + (classFieldAliasMappings == null ? 0 : classFieldAliasMappings.hashCode());
        result = prime * result + (dbStatementsMapping == null ? 0 : dbStatementsMapping.hashCode());
        result = prime * result + (entityMappings == null ? 0 : entityMappings.hashCode());
        result = prime * result + (mappers == null ? 0 : mappers.hashCode());
        result = prime * result + (sequencesMappings == null ? 0 : sequencesMappings.hashCode());
        result = prime * result + (statementMapping == null ? 0 : statementMapping.hashCode());
        result = prime * result + (typeAliases == null ? 0 : typeAliases.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractMyBatisConfiguration other = (AbstractMyBatisConfiguration) obj;
        if (classAliasMappings == null) {
            if (other.classAliasMappings != null) {
                return false;
            }
        } else if (!classAliasMappings.equals(other.classAliasMappings)) {
            return false;
        }
        if (classFieldAliasMappings == null) {
            if (other.classFieldAliasMappings != null) {
                return false;
            }
        } else if (!classFieldAliasMappings.equals(other.classFieldAliasMappings)) {
            return false;
        }
        if (dbStatementsMapping == null) {
            if (other.dbStatementsMapping != null) {
                return false;
            }
        } else if (!dbStatementsMapping.equals(other.dbStatementsMapping)) {
            return false;
        }
        if (entityMappings == null) {
            if (other.entityMappings != null) {
                return false;
            }
        } else if (!entityMappings.equals(other.entityMappings)) {
            return false;
        }
        if (mappers == null) {
            if (other.mappers != null) {
                return false;
            }
        } else if (!mappers.equals(other.mappers)) {
            return false;
        }
        if (sequencesMappings == null) {
            if (other.sequencesMappings != null) {
                return false;
            }
        } else if (!sequencesMappings.equals(other.sequencesMappings)) {
            return false;
        }
        if (statementMapping == null) {
            if (other.statementMapping != null) {
                return false;
            }
        } else if (!statementMapping.equals(other.statementMapping)) {
            return false;
        }
        if (typeAliases == null) {
            if (other.typeAliases != null) {
                return false;
            }
        } else if (!typeAliases.equals(other.typeAliases)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AbstractMyBatisConfiguration [mappers=" + mappers + "]";
    }

}
