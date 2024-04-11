<script>
    import { enhance } from '$app/forms';
    /** @type {import('./$types').ActionData} */
	export let form;
</script>

<svelte:head>
	<title>Register</title>
</svelte:head>

<div>
    <form use:enhance method="POST">
        <fieldset>
            <input name="name" type="text" required placeholder="Username"/>
        </fieldset>
        <fieldset>
            <input name="email" type="email" required placeholder="Email"/>
        </fieldset>
        <fieldset>
            <input name="password" type="password" required placeholder="Password"/>
        </fieldset>
        <button type="submit">Register</button>
    </form>
</div>