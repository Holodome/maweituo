<script>
    import { page } from '$app/stores';
    import { enhance } from '$app/forms';
	/** @type {import('./$types').PageData} */
	export let data;
</script>

<div>
    <h1>Chat</h1>
    <p><a href="/ads/{$page.params.ad}">Advertisement</a></p>

    {#each data.messages as msg}
        <p>
            {msg.sender} said: {msg.text}
        </p>
    {/each}

    <form use:enhance action="?/send_message" method="POST">
        <textarea name="text" rows=5></textarea>
        <button type="submit">Send</button>
    </form>
</div>