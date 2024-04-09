<script>
    import { page } from '$app/stores';
</script>

<nav>
    <a href="/">Home</a>

    {#if $page.data.user}
        <a href="/account">Account</a>
    {:else}
        <a href="/login">Login</a>
        <a href="/register">Register</a>
    {/if}
</nav>