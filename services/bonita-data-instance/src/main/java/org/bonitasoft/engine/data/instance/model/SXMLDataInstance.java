/**
 * Copyright (C) 2019 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.engine.data.instance.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bonitasoft.engine.data.definition.model.SXMLDataDefinition;
import org.hibernate.annotations.Type;

/**
 * @author Elias Ricken de Medeiros
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("SXMLDataInstanceImpl")
public class SXMLDataInstance extends SDataInstance {


    @Column(name = "clobValue")
    @Type(type = "materialized_clob")
    private String value;
	@Column
    private String namespace;
	@Column
    private String element;

    public SXMLDataInstance(final SXMLDataDefinition dataDefinition) {
        super(dataDefinition);
        namespace = dataDefinition.getNamespace();
        element = dataDefinition.getElement();
    }

    @Override
    public void setValue(final Serializable value) {
        this.value = (String) value;
    }

}
