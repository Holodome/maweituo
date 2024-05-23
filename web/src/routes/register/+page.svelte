<script>
  import { enhance } from '$app/forms';
  /** @type {import('./$types').ActionData} */
  export let form;
</script>

<svelte:head>
  <title>Register</title>
</svelte:head>

<div class="card shrink-0 w-full max-w-sm shadow-2xl bg-base-100 mx-auto">
  <form class="card-body" use:enhance method="POST">
    <h3>Register</h3>
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
      <label class="label" for="email">
        <span class="label-text">Email</span>
      </label>
      <input
        id="email"
        name="email"
        type="email"
        placeholder="email"
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
      <button class="btn btn-primary" type="submit">Register</button>
    </div>
    <div class="label">
      <span class="label-text-alt">
        Already have an account?
        <a href="/login" class="link link-neutral">Sign in</a>
      </span>
    </div>
  </form>
</div>
