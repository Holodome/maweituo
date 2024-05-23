<script>
  import { enhance } from '$app/forms';
  /** @type {import('./$types').ActionData} */
  export let form;
</script>

<svelte:head>
  <title>Sign in</title>
</svelte:head>

<div class="card shrink-0 w-full max-w-sm shadow-2xl bg-base-100 mx-auto">
  <form class="card-body" use:enhance method="POST">
    <h3>Sign in</h3>
    <div class="form-control">
      <label class="label" for="name">
        <span class="label-text">Name</span>
      </label>
      <input
        id="name"
        name="name"
        type="text"
        placeholder="name"
        class="input input-bordered"
        required
      />
    </div>
    <div class="form-control">
      <label class="label" for="password">
        <span class="label-text">Password</span>
      </label>
      <input
        id="password"
        name="password"
        type="password"
        placeholder="password"
        class="input input-bordered"
        required
      />
    </div>
    <div class="form-control mt-6">
      <button class="btn btn-primary" type="submit">Sign in</button>
    </div>
    <div class="label">
      <span class="label-text-alt">
        Don't have an account yet?
        <a href="/register" class="link link-neutral">Register now</a>
      </span>
    </div>
  </form>
</div>
