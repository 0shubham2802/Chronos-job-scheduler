package com.chronos.chronos.config;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class PostgreSQLEnumType implements UserType<Enum> {

    @Override
    public int getSqlType() {
        return SqlTypes.OTHER;
    }

    @Override
    public Class<Enum> returnedClass() {
        return Enum.class;
    }

    @Override
    public boolean equals(Enum x, Enum y) {
        return x == y;
    }

    @Override
    public int hashCode(Enum x) {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Enum nullSafeGet(ResultSet rs, int position,
                            SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        return null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Enum value, int index,
                            SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, value.name(), Types.OTHER);
        }
    }

    @Override
    public Enum deepCopy(Enum value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Enum value) {
        return value == null ? null : value.name();
    }

    @Override
    public Enum assemble(Serializable cached, Object owner) {
        return null;
    }
}
