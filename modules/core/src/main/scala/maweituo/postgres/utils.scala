package maweituo.postgres.utils

import maweituo.domain.ads.AdSortOrder
import doobie.util.fragment.Fragment
import doobie.syntax.all.*

def adSortOrderToSql(order: AdSortOrder): Fragment =
  order match
    case AdSortOrder.CreatedAtAsc => fr"order by created_at asc"
    case AdSortOrder.UpdatedAtAsc => fr"order by updated_at desc"
    case AdSortOrder.Alphabetic   => fr"order by title asc"
    case AdSortOrder.Author       => fr"order by (select name from users where id = author_id)"
