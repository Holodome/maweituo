<script>
  import { page } from '$app/stores';
</script>

<div class="navbar bg-base-100">
  <div class="flex-1">
    <a class="btn btn-ghost text-xl" href="/">Maweituo</a>
  </div>

  <div class="flex-none">
    <ul class="menu menu-horizontal px-1">
      {#if $page.data.user}
        <li><a href="/create_ad">Create advertisement</a></li>
        <li><a href="/account">Account</a></li>
      {:else}
        <li><a href="/login">Login</a></li>
        <li><a href="/register">Register</a></li>
      {/if}
    </ul>
  </div>
</div>
