<script>
    import { enhance } from '$app/forms';
    /** @type {import('./$types').ActionData} */
	export let form;
</script>

<svelte:head>
	<title>Sign in</title>
</svelte:head>

<div>
    <form use:enhance method="POST">
        <fieldset>
            <input name="username" type="text" required placeholder="Username"/>
        </fieldset>
        <fieldset>
            <input name="password" type="password" required placeholder="Password"/>
        </fieldset>
        <button type="submit">Login</button>
    </form>
</div>