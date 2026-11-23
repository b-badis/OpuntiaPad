package io.github.bbadis.opuntiapad.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlFormatterTest {

    @Test
    fun `puts clauses on their own lines and upper-cases keywords`() {
        val input = "select u.id, u.name, count(*) as total from users u left join orders o on o.user_id = u.id " +
            "where u.active = true and o.amount > 10 group by u.id, u.name order by total desc limit 10;"
        val expected = """
            SELECT u.id,
              u.name,
              COUNT(*) AS total
            FROM users u
            LEFT JOIN orders o
              ON o.user_id = u.id
            WHERE u.active = TRUE
              AND o.amount > 10
            GROUP BY u.id,
              u.name
            ORDER BY total DESC
            LIMIT 10;
        """.trimIndent()
        assertEquals(expected, SqlFormatter.format(input))
    }

    @Test
    fun `never changes strings, quoted identifiers or comments`() {
        val input = "select 'it''s  where' as \"Mixed Case\", `col` from t -- trailing select\n/* keep  this */ where x='from'"
        val out = SqlFormatter.format(input)
        listOf("'it''s  where'", "\"Mixed Case\"", "`col`", "-- trailing select", "/* keep  this */", "'from'").forEach {
            assertTrue("$it missing in:\n$out", out.contains(it))
        }
    }

    @Test
    fun `only whitespace and keyword case change`() {
        val input = "insert into t (a, b) values (1, 'x'), (2.5, 'y'); update t set a = a + 1 where b in (select b from u);"
        val normalize = { s: String -> s.filterNot { it.isWhitespace() }.lowercase() }
        assertEquals(normalize(input), normalize(SqlFormatter.format(input)))
    }

    @Test
    fun `indents sub-queries`() {
        val out = SqlFormatter.format("select * from (select id from t where x = 1) s")
        assertTrue(out, out.contains("FROM (\n  SELECT id\n  FROM t\n  WHERE x = 1\n) s"))
    }
}
