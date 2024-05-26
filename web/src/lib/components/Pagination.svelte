<script lang="ts">
  export let totalCount: number;
  export let currentPage: number;
  const perPage: number = 10;

  $: currentPage = 0;
  $: totalPages = Math.ceil(totalCount / perPage);
  $: start = currentPage * perPage;
  $: end = currentPage === totalPages - 1 ? totalCount - 1 : start + perPage - 1;

  $: (currentPage = 0);
  $: currentPage, start, end;
</script>

<div></div>
