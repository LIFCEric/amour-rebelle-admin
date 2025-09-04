package fr.amour.rebelle.admin.dao;

import java.sql.Connection;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Utility class responsible for looking up and providing access to the
 * application's {@link DataSource}. The data source is retrieved through JNDI
 * and cached for subsequent use. Convenience methods are also supplied to run
 * code with a pooled {@link Connection}.
 */
public final class DataSourceProvider {

    /** Default JNDI name used to look up the data source. */
    public static final String JNDI = "JDBC/FRENCHY";

    /** Cached singleton {@link DataSource} instance. */
    private static volatile DataSource dataSource;

    /** Private constructor to prevent instantiation. */
    private DataSourceProvider() {}

    /**
     * Returns the {@link DataSource} bound to the supplied JNDI name. The
     * lookup is performed lazily and the result cached for later calls.
     *
     * @param jndi the JNDI name of the data source
     * @return the matching {@link DataSource}
     * @throws NamingException if the JNDI resource cannot be found or is not a
     *                         {@link DataSource}
     */
    public static DataSource get(String jndi) throws NamingException {
        if (dataSource == null) {
            synchronized (DataSourceProvider.class) {
                if (dataSource == null) {
                    InitialContext ctx = null;
                    Context env = null;
                    try {
                        ctx = new InitialContext();
                        env = (Context) ctx.lookup("java:/comp/env");
                        Object ds = env.lookup(jndi);
                        if (!(ds instanceof DataSource)) {
                            throw new NamingException("L'objet JNDI '" + jndi + "' n'est pas un DataSource");
                        }
                        dataSource = (DataSource) ds;
                    } finally {
                        if (env != null) try { env.close(); } catch (NamingException ignored) {}
                        if (ctx != null) try { ctx.close(); } catch (NamingException ignored) {}
                    }
                }
            }
        }
        return dataSource;
    }

    /**
     * Returns the application's default {@link DataSource}.
     *
     * @return the default data source
     * @throws NamingException if the JNDI lookup fails
     */
    public static DataSource getDefault() throws NamingException {
        return get(JNDI);
    }

    /**
     * Executes work with a {@link Connection} from the pool. The connection is
     * automatically closed after the callback completes.
     *
     * @param <T>  type of result produced by the work
     * @param jndi the JNDI name of the data source
     * @param work callback executed with a pooled connection
     * @return the value returned by the callback
     * @throws Exception if thrown by the callback or during access
     */
    public static <T> T withConnection(String jndi, SQLFunction<Connection, T> work) throws Exception {
        DataSource ds = get(jndi);
        try (Connection c = ds.getConnection()) {
            return work.apply(c);
        }
    }

}

