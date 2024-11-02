import { render, ComponentChildren } from "preact";
import { useEffect, useErrorBoundary, useState } from "preact/hooks";
import { signal } from "@preact/signals";
import {
  LocationProvider,
  Router,
  Route,
  useLocation,
} from "preact-iso";
import { JSX } from "preact/jsx-runtime";
import { trpc } from "./client";
import { Post, ValidationErrorData } from "./types";
import { TRPCClientError } from "@trpc/client";
import { AppRouter } from "../server";

const isNotFoundError = (error?: TRPCClientError<AppRouter>) => error?.data?.code === "NOT_FOUND"

function whenError<T>(predicate: (error: T) => boolean, matchHandler: (error: T) => any) {
  return (error: T) => {
    if (predicate(error)) {
      return matchHandler(error)
    }
    throw error
  }
}

function usePromise<T>(callback: () => Promise<T>, deps: ReadonlyArray<unknown> = []) {
  const [isLoading, setLoading] = useState(true);
  const [value, setValue] = useState<T | null>(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const promise = callback();

    promise
      .then(
        (value) => setValue(value),
        (error) => setError(error)
      )
      .then(() => setLoading(false));

    setLoading(true);
  }, [...deps]);

  if (error)
    throw error

  return { isLoading, value };
};

function PostList() {
  const {
    value: posts,
    isLoading,
  } = usePromise(() => trpc.post.list.query());

  if (isLoading || !posts) return <Loading />;

  return (
    <div>
      {posts.map((post: Post) => (
        <section key={post.id}>
          <h3>
            <a href={`/posts/${post.id}`}>{post.title}</a>
          </h3>
        </section>
      ))}
      {!posts.length && <h4>No posts published yet</h4>}
    </div>
  );
}

function PenToSquare() {
  const size = "20px";
  return (
    <svg
      width={size}
      height={size}
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 512 512"
    >
      <path d="M441 58.9L453.1 71c9.4 9.4 9.4 24.6 0 33.9L424 134.1 377.9 88 407 58.9c9.4-9.4 24.6-9.4 33.9 0zM209.8 256.2L344 121.9 390.1 168 255.8 302.2c-2.9 2.9-6.5 5-10.4 6.1l-58.5 16.7 16.7-58.5c1.1-3.9 3.2-7.5 6.1-10.4zM373.1 25L175.8 222.2c-8.7 8.7-15 19.4-18.3 31.1l-28.6 100c-2.4 8.4-.1 17.4 6.1 23.6s15.2 8.5 23.6 6.1l100-28.6c11.8-3.4 22.5-9.7 31.1-18.3L487 138.9c28.1-28.1 28.1-73.7 0-101.8L474.9 25C446.8-3.1 401.2-3.1 373.1 25zM88 64C39.4 64 0 103.4 0 152L0 424c0 48.6 39.4 88 88 88l272 0c48.6 0 88-39.4 88-88l0-112c0-13.3-10.7-24-24-24s-24 10.7-24 24l0 112c0 22.1-17.9 40-40 40L88 464c-22.1 0-40-17.9-40-40l0-272c0-22.1 17.9-40 40-40l112 0c13.3 0 24-10.7 24-24s-10.7-24-24-24L88 64z" />
    </svg>
  );
}

function FormError({ errors }: { errors: string[] }) {
  return (
    <>
      {errors?.map((error) => (
        <div className="invalid-feedback" key={error}>
          {error}
        </div>
      ))}
    </>
  );
}

type PostFormParams = {
  post?: Post
  onSuccess: (value: any) => void
  onSubmit: (formValues: any) => Promise<Post>
  cancelUrl: string
}

function PostForm({ post, onSuccess, onSubmit, cancelUrl }: PostFormParams) {
  type NormalizedErrors = {
    [key: string]: string[]
  };

  const [isSubmitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<NormalizedErrors>({});

  const handleError = (trpcClientError: TRPCClientError<AppRouter>) => {
    if (trpcClientError.data?.code === "UNPROCESSABLE_CONTENT") {
      const data = trpcClientError.data as ValidationErrorData
      const errors = data.errors.reduce<NormalizedErrors>((memo, error) => {
        memo[error.field] ||= [];
        memo[error.field].push(error.message);
        return memo;
      }, {});
      setErrors(errors);
      alerts.show("danger", trpcClientError.message);
    } else {
      throw trpcClientError
    }
  };

  const handleSubmit = (evt: JSX.TargetedSubmitEvent<HTMLFormElement>) => {
    evt.preventDefault();
    setSubmitting(true);

    const newPost = Object.fromEntries(new FormData(evt.target as HTMLFormElement));

    // for (const key of Object.keys(newPost)) {
    //   if (typeof newPost[key] === 'string' && newPost[key].length === 0) {
    //     newPost[key] = null as any
    //   }
    // }

    onSubmit(newPost)
      .then(onSuccess, handleError)
      .finally(() => setSubmitting(false));
  };

  return (
    <form onSubmit={handleSubmit} method="post" action="">
      <div className="mb-3">
        <label htmlFor="title" className="form-label">
          Title
        </label>
        <input
          id="title"
          className={`form-control ${errors.title ? "is-invalid" : ""}`}
          type="text"
          name="title"
          defaultValue={post?.title}
        />
        <FormError errors={errors.title} />
      </div>
      <div className="mb-3">
        <label htmlFor="content" className="form-label">
          Content
        </label>
        <textarea
          id="content"
          className={`form-control ${errors.content ? "is-invalid" : ""}`}
          name="content"
          defaultValue={post?.content}
          rows={3}
        />
        <FormError errors={errors.content} />
      </div>
      <div>
        <button
          className="btn btn-primary"
          type="submit"
          disabled={isSubmitting}
        >
          Save
        </button>
        <a class="btn btn-link" href={cancelUrl}>
          Cancel
        </a>
      </div>
    </form>
  );
}

function PostEdit({ id }: { id: string }) {
  const { route } = useLocation();
  const {
    value: post,
    isLoading,
  } = usePromise(() => trpc.post.find.query(id).catch(whenError(isNotFoundError, () => {
      alerts.show("warning", "Post not found");
      route("/posts");
  })), [id]);

  if (isLoading) return <Loading />;

  return (
    <div>
      <PostForm
        post={post}
        onSubmit={(updatedPost) => trpc.post.update.mutate({ id: Number(id), ...updatedPost })}
        onSuccess={() => {
          alerts.show("success", "Post updated successfully");
          route(`/posts/${id}`);
        }}
        cancelUrl={`/posts/${id}`}
      />
    </div>
  );
}

function PostNew() {
  const { route } = useLocation();

  return (
    <div>
      <PostForm
        onSubmit={(newPost) => trpc.post.create.mutate(newPost)}
        onSuccess={({ id }) => {
          alerts.show("success", "Post created successfully");
          route(`/posts/${id}`);
        }}
        cancelUrl="/posts"
      />
    </div>
  );
}

function PostShow({ id }: { id: string }) {
  const { route } = useLocation();
  const {
    value: post,
    isLoading,
  } = usePromise(() => trpc.post.find.query(id).catch(whenError(isNotFoundError, () => {
      alerts.show("warning", "Post not found");
      route("/posts");
  })), [id]);

  if (isLoading) return <Loading />;

  const handleDelete = () => {
    if (confirm("Are you sure?")) {
      trpc.post.destroy.mutate(id).then(() => route("/posts"))
    }
  };

  return (
    <div>
      <section>
        <h3>
          <span>{post.title}</span>
          <a class="ms-2" href={`/posts/${post.id}/edit`}>
            <PenToSquare />
          </a>
        </h3>
        <p>{post.content}</p>
      </section>
      <div class="mb-2 d-flex align-items-center">
        <a class="me-4" href="/posts">
          See all posts
        </a>
        <button
          class="btn btn-danger ms-auto"
          onClick={handleDelete}
          type="button"
        >
          Delete
        </button>
      </div>
    </div>
  );
}

function Loading() {
  return <h3>Loading ...</h3>;
}

type AlertType = 'success' | 'warning' | 'danger'

type AlertPayload = {
  id: number
  type: AlertType
  content: string
}

const alerts = {
  nextId: 0,
  current: signal<AlertPayload[]>([]),
  show(type: AlertType, content: string) {
    this.current.value = [
      ...this.current.value,
      { id: this.nextId++, type, content },
    ];
  },
  remove(id: number) {
    this.current.value = this.current.value.filter((item) => item.id !== id);
  },
};

function AlertManager() {
  return (
    <div>
      {alerts.current.value.map((entry) => (
        <Alert key={entry.id} id={entry.id} type={entry.type}>
          {entry.content}
        </Alert>
      ))}
    </div>
  );
}

type AlertParams = {
  id?: number
  children: ComponentChildren
  type: AlertType
}

function Alert({ id, children, type = "success" }: AlertParams) {
  useEffect(() => {
    if (typeof id === 'number') {
      setTimeout(() => {
        alerts.remove(id);
      }, 3000);
    }
  }, [id]);

  return (
    <div class={`alert alert-${type} alert-dismissible fade show`} role="alert">
      {children}
      <button
        type="button"
        class="btn-close"
        data-bs-dismiss="alert"
        aria-label="Close"
      ></button>
    </div>
  );
}

function NotFound() {
  const { route } = useLocation();

  useEffect(() => {
    alerts.show("warning", "Page not found");
    route("/posts");
  }, []);

  return null
}

function UncaughtError({ error }: { error: TRPCClientError<AppRouter> }) {
  return (
    <Alert type="danger">
      <h4 class="alert-heading">
        <svg
          width={18}
          height={18}
          xmlns="http://www.w3.org/2000/svg"
          class="bi bi-exclamation-triangle-fill flex-shrink-0 me-2"
          viewBox="0 0 16 16"
          role="img"
          aria-label="Warning:"
        >
          <path d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z" />
        </svg>
        {error.message}
      </h4>
      <hr />
      {error.data ? (
        <div>
          <div>
            <strong class="me-2">Code:</strong><span>{error.data.code}</span>
          </div>
          <div>
            <strong class="me-2">HTTP Status:</strong><span>{error.data.httpStatus}</span>
          </div>
          <div>
            <strong class="me-2">Path:</strong><span>{error.data.path}</span>
          </div>
          <pre class="mt-2">{error.data.stack}</pre>
        </div>
      ) : null}
    </Alert>
  );
}

function App() {
  const [error] = useErrorBoundary()

  return (
    <main className="container">
      <nav class="navbar navbar-expand-lg">
        <div class="container-fluid">
          <a class="navbar-brand" href="/">
            Blog
          </a>
          <a href="/posts/new">Write a new post</a>
        </div>
      </nav>
      <div class="mt-4">
        {error ? <UncaughtError error={error} /> : null}
        <AlertManager />
        <LocationProvider>
          <Router>
            <Route path="/" component={PostList} />
            <Route path="/posts" component={PostList} />
            <Route path="/posts/new" component={PostNew} />
            <Route path="/posts/:id" component={PostShow} />
            <Route path="/posts/:id/edit" component={PostEdit} />
            <Route default component={NotFound} />
          </Router>
        </LocationProvider>
      </div>
    </main>
  );
}

render(<App />, document.getElementById('app')!)
